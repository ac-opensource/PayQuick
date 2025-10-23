
## Frontend Engineering Challenge Mock API

A simple Node + Express JS application that provides mock API for the frontend engineering challenge.

- Node v22.20.0
- Express 5.1.0

To run
- npm install
- npm run dev

### API Paths

#### `POST v1/login`
##### Body: `{ "email": "smith@example.com", "password": "pass123" }`
##### Response:
```
{
  "status": "success",
  "message": "Authentication successful",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
    "expires_in": 900,
    "refresh_token": "rft_0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d",
    "token_type": "Bearer",
    "user": {
      "user_id": "usr_a1b2c3d4e5f6",
      "full_name": "Paul Smith",
      "email": "smith@example.com"
    }
  }
}
 ```
---
#### `POST v1/token/refresh`
##### Body:  `"refresh_token":  "token"`
##### Response:
```{
  "status": "success",
  "message": "Note: refresh_token is picked randomly from a list. Returned value might be the same as previous.",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
    "expires_in": 900,
    "token_type": "Bearer",
    "user": {
      "user_id": "usr_a1b2c3d4e5f6",
      "full_name": "Paul Smith",
      "email": "smith@example.com"
    }
  }
}
```
---
###  `GET v1/transactions?page=1`
#####  Query Parameters: `page = pageNumber `
#####  Response:
```{
  "status": "success",
  "message": "Returning items 1-5",
  "pagination": {
    "current_page": 1,
    "total_pages": 2,
    "total_items": 10,
    "items_per_page": 5
  },
  "data": [
    {
      "id": "txn_abc123def456",
      "amount_in_cents": 5000,
      "currency": "USD",
      "type": "TRANSFER",
      "status": "SUCCESS",
      "created_at": "2025-10-09T10:30:00Z",
      "destination_id": "wal_20251009-TRF5"
    }
  ]
}
```