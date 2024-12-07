# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "pyjwt",
#     "python-dotenv",
# ]
# ///

import jwt
from datetime import datetime, timedelta, timezone
import os
from dotenv import load_dotenv

# Define your secret and algorithm
load_dotenv()
SECRET = os.getenv("JWT_SECRET")
ALGORITHM = "HS256"


# Define the claims for the token
def generate_jwt():
    payload = {
        "sub": "axel",  # User ID or subject
        "aud": "listan",  # Audience
        "exp": datetime.now(timezone.utc)
        + timedelta(minutes=5),  # Expiration in 5 minutes
        "iat": datetime.now(timezone.utc),  # Issued at
    }

    # Generate the token
    token = jwt.encode(payload, SECRET, algorithm=ALGORITHM)
    return token


if __name__ == "__main__":
    token = generate_jwt()
    print(token)

