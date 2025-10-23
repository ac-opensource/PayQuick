import express from 'express';
import type { Application, Request, Response } from 'express';
import apiRoutes from './routes/apiRoutes.js';

const app: Application = express();
const PORT = process.env.PORT || 3000;

app.use(express.json())

app.get('/', (req: Request, res: Response) => {
    res.send("API is up and running! 🚀")
})

app.listen(PORT, () => {
    console.log(`Server is listening to port ${PORT}`);    
})

app.use('/api', apiRoutes);


