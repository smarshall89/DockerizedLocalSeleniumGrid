# dockerizedSeleniumGrid

Quick Start Guide:

1) Install docker for desktop (https://www.docker.com/products/docker-desktop)
2) Run Main.java as Java Application
  - Lines 30 - 35 will create a hub and a single chrome node as docker containers
  - Lines 37 and 38 will stop the containers
  - Lines 40 and 41 will remove the containers
  
Debug Main.java with a breakpoint at line 37 (just before the containers are stopped), then navigate to (http://localhost:4444/grid/console) to see selenium grid console.
