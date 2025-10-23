import type { Request, Response } from 'express';

export const UNIVERSAL_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

interface RefreshTokenBody {
    refresh_token: string
}

export const refresh = (req: Request<{}, {}, RefreshTokenBody>, res: Response) => {
    const { refresh_token } = req.body;
    const authHeader = req.headers['authorization'];
    const accessToken = authHeader && authHeader.split(' ')[1];

    if (accessToken != UNIVERSAL_ACCESS_TOKEN) {
        return res.sendStatus(401);
    } 
    
    const validRefreshTokens: string[] = [
        "rft_0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d",
        "rft_0a9b8c7d6e5f4a3b2c1d0e9f8a7b6909",
        "rft_0a9b8c7d6e5f4a3b2c1d0e9f8a7b6111"
    ];

    const getRandomToken = () => {
        return validRefreshTokens[Math.floor(Math.random() * validRefreshTokens.length)] ?? validRefreshTokens[0]!
    };

    if (!validRefreshTokens.includes(refresh_token)) {
        return res.status(401).json({
            'status': 'Failure',
            'message': 'Refresh token is invalid.'
        });
    }

    res.status(200).json({
        'status': 'success',
        'message': 'Note: refresh_token is picked randomly from a list. Returned value might be the same as previous.',
        'data': {
            'access_token': UNIVERSAL_ACCESS_TOKEN,
            'expires_in': 900, // 15 minutes
            'refresh_token': getRandomToken(),
            'token_type': "Bearer",
            'user': {
                "user_id": "usr_a1b2c3d4e5f6",
                "full_name": "Paul Smith",
                "email": "smith@example.com"
            }
        }
    });
};