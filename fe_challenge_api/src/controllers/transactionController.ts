import type { Request, Response } from 'express';
import { UNIVERSAL_ACCESS_TOKEN } from './tokenController.js';

export const getTransactions = (req: Request, res: Response) => {

    const authHeader = req.headers['authorization'];
    const accessToken = authHeader && authHeader.split(' ')[1];

    // If no token is provided, deny access
    if (accessToken != UNIVERSAL_ACCESS_TOKEN) {
        return res.sendStatus(401); // Unauthorized
    }

    let page = parseInt(req.query.page as string, 10);


    if (isNaN(page)) {
        page = 1;
    }

    switch (page) {
        case 1: {
            return res.status(200).json(transactions[0]);
        }
        case 2: {
            return res.status(200).json(transactions[1]);
        }
        default: {
            return res.status(200).json({
                status: 'success',
                message: 'No more items to return',
                pagination: {
                    current_page: page,
                    total_pages: 2,
                    total_items: 10,
                    items_per_page: 5
                },
                data: []
            });
        }
    }
};

//  Response data

type TransactionResponse = {
    status: string,
    message: string,
    pagination: {
        current_page: number,
        total_pages: number,
        total_items: number,
        items_per_page: number
    },
    data: Transaction[]
}

type Transaction = {
    id: string,
    amount_in_cents: number,
    currency: string,
    type: 'TRANSFER' | 'TOPUP',
    status: string,
    created_at: string,
    destination_id: string
}

const transactions: TransactionResponse[] = [
    { // p1, 1-5
        status: 'success',
        message: 'Returning items 1-5',
        pagination: {
            current_page: 1,
            total_pages: 2,
            total_items: 10,
            items_per_page: 5
        },
        data: [
            {
                id: "txn_abc123def456",
                amount_in_cents: 5000,
                currency: "USD",
                type: 'TRANSFER',
                status: "SUCCESS",
                created_at: "2025-10-09T10:30:00Z",
                destination_id: "wal_20251009-TRF5"

            },
            {
                id: "txn_ab94430r",
                amount_in_cents: 300,
                currency: "USD",
                type: 'TRANSFER',
                status: "SUCCESS",
                created_at: "2025-10-09T10:30:00Z",
                destination_id: "wal_20251009-TRF4"
            },
            {
                id: "txn_abc24adf536",
                amount_in_cents: 10000,
                currency: "USD",
                type: "TOPUP",
                status: "SUCCESS",
                created_at: "2025-09-09T10:30:00Z",

                destination_id: "wal_20251009-001TP"

            },
            {
                id: "txn_abc123def4123156",
                amount_in_cents: 5025,
                currency: "USD",
                type: "TRANSFER",
                status: "SUCCESS",
                created_at: "2025-08-09T10:30:00Z",
                destination_id: "wal_20251009-TRF3"
            },
            {
                id: "txn_abc123def8456",
                amount_in_cents: 3200,
                currency: "USD",
                type: "TOPUP",
                status: "SUCCESS",
                created_at: "2025-08-09T10:30:00Z",
                destination_id: "wal_20251009-001TP"

            }
        ]
    },

    // p2, 6-10
    {
        status: 'success',
        message: 'Returning items 6-10',
        pagination: {
            current_page: 2,
            total_pages: 2,
            total_items: 10,
            items_per_page: 5
        },
        data: [
            {
                id: "txn_abc123def45600",
                amount_in_cents: 5020,
                currency: "USD",
                type: "TRANSFER",
                status: "SUCCESS",
                created_at: "2025-06-20T12:30:00Z",
                destination_id: "wal_20251009-TRF2"
            },
            {
                id: "txn_aoe123def456",
                amount_in_cents: 6000,
                currency: "USD",
                type: "TRANSFER",
                status: "SUCCESS",
                created_at: "2025-06-12T10:30:00Z",
                destination_id: "wal_20251009-TRF1"
            },
            {
                id: "txn_opql123def456",
                amount_in_cents: 8000,
                currency: "USD",
                type: "TOPUP",
                status: "SUCCESS",
                created_at: "2025-05-09T10:40:00Z",
                destination_id: "wal_20251009-001TP"
            },
            {
                id: "txn_aksmc123def456",
                amount_in_cents: 9130,
                currency: "USD",
                type: "TOPUP",
                status: "SUCCESS",
                created_at: "2025-05-09T10:15:00Z",
                destination_id: "wal_20251009-001TP"
            }
        ]
    }
];