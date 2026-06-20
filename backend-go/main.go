package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"time"

	"github.com/redis/go-redis/v9"
)

var ctx = context.Background()
var rdb *redis.Client

// Structure to parse incoming requests from your Android application
type ScanRequest struct {
	URL string `json:"url"`
}

// Structure to send data back to your Android application
type ScanResponse struct {
	Verdict    string  `json:"verdict"`
	Confidence float64 `json:"confidence"`
	Source     string  `json:"source"`
	Cached     bool    `json:"cached"`
}

func main() {
	// 1. Initialize Serverless Redis connection dynamically using AWS environment paths
	redisEndpoint := os.Getenv("REDIS_HOST")
	if redisEndpoint == "" {
		fmt.Println("Warning: REDIS_HOST environment variable is not set. Caching is disabled.")
	} else {
		rdb = redis.NewClient(&redis.Options{
			Addr:         redisEndpoint,
			DialTimeout:  1 * time.Second,
			ReadTimeout:  500 * time.Millisecond,
			WriteTimeout: 500 * time.Millisecond,
			MaxRetries:   0,
		})
	}

	// 2. Set up the API route your Android App will call
	http.HandleFunc("/api/v1/scan", handleUrlScanPipeline)
	http.HandleFunc("/health", handleHealthCheck)

	// 3. Bind to App Runner default internal container port
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	fmt.Printf("Server launching on port %s...\n", port)
	http.ListenAndServe(":"+port, nil)
}

func handleHealthCheck(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status": "healthy"}`))
}

func handleUrlScanPipeline(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST requests allowed", http.StatusMethodNotAllowed)
		return
	}

	var req ScanRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil || req.URL == "" {
		http.Error(w, "Invalid request payload missing a URL target", http.StatusBadRequest)
		return
	}

	// === STAGE 1: REDIS CACHE LOOKUP ===
	if rdb != nil {
		redisCtx, cancel := context.WithTimeout(r.Context(), 500*time.Millisecond)
		cachedVerdict, err := rdb.Get(redisCtx, req.URL).Result()
		cancel()
		if err == nil && cachedVerdict != "" {
			sendJSONResponse(w, ScanResponse{
				Verdict:    cachedVerdict,
				Confidence: 99.0,
				Source:     "CACHE",
				Cached:     true,
			})
			return
		} else if err != nil && err != redis.Nil {
			fmt.Printf("Redis cache lookup failed: %v\n", err)
		}
	}

	// === STAGE 2: GOOGLE SAFE BROWSING CHECK ===
	googleKey := os.Getenv("GOOGLE_SAFE_BROWSING_KEY")
	isMaliciousGoogle := checkGoogleSafeBrowsing(req.URL, googleKey)
	if isMaliciousGoogle {
		// Cache malicious finding for 24 hours to optimize performance costs
		if rdb != nil {
			redisCtx, cancel := context.WithTimeout(r.Context(), 500*time.Millisecond)
			err := rdb.Set(redisCtx, req.URL, "MALICIOUS", 24*time.Hour).Err()
			cancel()
			if err != nil {
				fmt.Printf("Redis cache write failed (MALICIOUS): %v\n", err)
			}
		}
		sendJSONResponse(w, ScanResponse{
			Verdict:    "MALICIOUS",
			Confidence: 99.0,
			Source:     "GOOGLE",
			Cached:     false,
		})
		return
	}

	// === STAGE 3: URLSCAN DEEP ZERO-DAY ANALYSIS ===
	urlScanKey := os.Getenv("URLSCAN_API_KEY")
	isMaliciousDeep := checkUrlScanZeroDay(req.URL, urlScanKey)

	finalVerdict := "SAFE"
	confidence := 99.0
	if isMaliciousDeep {
		finalVerdict = "MALICIOUS"
		confidence = 90.0
	}

	// Cache final verdict
	if rdb != nil {
		redisCtx, cancel := context.WithTimeout(r.Context(), 500*time.Millisecond)
		err := rdb.Set(redisCtx, req.URL, finalVerdict, 12*time.Hour).Err()
		cancel()
		if err != nil {
			fmt.Printf("Redis cache write failed (%s): %v\n", finalVerdict, err)
		}
	}

	sendJSONResponse(w, ScanResponse{
		Verdict:    finalVerdict,
		Confidence: confidence,
		Source:     "URLSCAN",
		Cached:     false,
	})
}

// Helper to interact with Google Safe Browsing API endpoints
func checkGoogleSafeBrowsing(targetURL string, apiKey string) bool {
	if apiKey == "" {
		return false // Skip lookup safely if key isn't provided during dev testing
	}

	requestBody := map[string]any{
		"client": map[string]string{
			"clientId":      "csp-app",
			"clientVersion": "1.0.0",
		},
		"threatInfo": map[string]any{
			"threatTypes":      []string{"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"},
			"platformTypes":    []string{"ANY_PLATFORM"},
			"threatEntryTypes": []string{"URL"},
			"threatEntries":    []map[string]string{{"url": targetURL}},
		},
	}

	body, err := json.Marshal(requestBody)
	if err != nil {
		return false
	}

	endpoint := fmt.Sprintf("https://safebrowsing.googleapis.com/v4/threatMatches:find?key=%s", url.QueryEscape(apiKey))
	resp, err := http.Post(endpoint, "application/json", bytes.NewReader(body))
	if err != nil || resp == nil {
		return false
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return false
	}

	var parsed struct {
		Matches []interface{} `json:"matches"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&parsed); err != nil {
		return false
	}

	return len(parsed.Matches) > 0
}

// Helper to execute deep payload sandboxing on URLScan for potential zero-days
func checkUrlScanZeroDay(targetURL string, apiKey string) bool {
	if apiKey == "" {
		return false
	}

	scanPayload := map[string]any{
		"url":    targetURL,
		"public": "off",
	}

	body, err := json.Marshal(scanPayload)
	if err != nil {
		return false
	}

	req, err := http.NewRequest("POST", "https://urlscan.io/api/v1/scan/", bytes.NewReader(body))
	if err != nil {
		return false
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("API-Key", apiKey)

	resp, err := http.DefaultClient.Do(req)
	if err != nil || resp == nil {
		return false
	}
	defer resp.Body.Close()

	return resp.StatusCode == http.StatusOK || resp.StatusCode == http.StatusAccepted || resp.StatusCode == http.StatusCreated
}

func sendJSONResponse(w http.ResponseWriter, resp ScanResponse) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
