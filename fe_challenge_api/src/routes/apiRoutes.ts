import { Router } from "express";
import { authenticate } from "../controllers/loginController.js";
import { refresh } from "../controllers/tokenController.js";
import { getTransactions } from "../controllers/transactionController.js";

const router = Router();
router.post('/v1/login', authenticate);
router.post('/v1/token/refresh', refresh);
router.get('/v1/transactions', getTransactions);

export default router;