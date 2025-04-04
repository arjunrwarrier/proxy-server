# Proxy Server Setup using Docker

This project sets up a proxy server running in a Docker container, which acts as a bridge between the proxy client and the internet. The proxy server is configured to run on port 9090. This guide will walk you through the steps required to set up a running environment for the proxy server using Docker.

TO SETUP A RUNNING ENVIRONMENT USING DOCKER
------------------------------
1. clone the project
2.DOCKER BUILD

`docker build -t proxy-server:latest .`

3.By default, Docker containers don’t share localhost unless they are on the same network.Create a custom docker network if not already created, make sure to run both in same network.
  docker network create proxy-network
4.Run both containers in same network exposing required ports
  docker run -d --name proxy-server --network=proxy-network -p 9090:9090 proxy-server:latest
5.Use curl command to check if the proxy is working, check logs of both server and client containers
  curl -x http://localhost:8080 http://httpforever.com/
