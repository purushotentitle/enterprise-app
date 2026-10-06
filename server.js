import http from 'node:http';

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({
    status: 'UP',
    service: 'payment-service',
    type: 'backend',
    infrastructure: {
      terraform: 'configured',
      helm: 'configured'
    }
  }));
});

server.listen(3000, '0.0.0.0', () => {
  console.log('Payment service listening on port 3000');
});
