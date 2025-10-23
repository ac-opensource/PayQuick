import type { Request, Response } from 'express';
import { UNIVERSAL_ACCESS_TOKEN } from './tokenController.js';

interface LoginReqBody {
    email: string
    password: string
}

export const authenticate = (req: Request<{}, {}, LoginReqBody>, res: Response) => {
    const { email, password } = req.body;

    if (email !== "smith@example.com" || password !== "pass123") {
        return res.status(401).json({
            'status': 'Failure',
            'message': 'Email or password is incorrect. Use email: smith@example.com, password: pass123'
        });
    }

    // This code will now only run if the 'if' condition was false
    res.status(200).json({
        'status': 'success',
        'message': 'Authentication successful',
        'data': {
            'access_token': UNIVERSAL_ACCESS_TOKEN,
            'expires_in': 900, // 15 minutes
            'refresh_token': "rft_0a9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d",
            'token_type': "Bearer",
            'user': {
                "user_id": "usr_a1b2c3d4e5f6",
                "full_name": "Paul Smith",
                "email": "smith@example.com"
            }
        }

    });

};