import re
from urllib.parse import urlparse, urlunparse

def normalize_url(url: str) -> str:
    """
    Normalizes a URL by converting to lowercase, removing trailing slashes,
    and standardizing the scheme.
    """
    url = url.strip().lower()

    # Add scheme if missing
    if not re.match(r'^https?://', url):
        url = 'http://' + url

    parsed = urlparse(url)

    # Remove default ports
    netloc = parsed.netloc
    if parsed.scheme == 'http' and netloc.endswith(':80'):
        netloc = netloc[:-3]
    elif parsed.scheme == 'https' and netloc.endswith(':443'):
        netloc = netloc[:-4]

    # Standardize path
    path = parsed.path
    if not path:
        path = '/'
    elif len(path) > 1 and path.endswith('/'):
        path = path[:-1]

    normalized = urlunparse((
        parsed.scheme,
        netloc,
        path,
        '', # params
        parsed.query,
        ''  # fragment
    ))

    return normalized
