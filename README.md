# Karnaugh Map Solver (Frontend + Java Backend)

An interactive web tool to create truth tables, visualize Karnaugh maps (2–5 variables), minimize boolean expressions (SOP and POS), and generate Verilog code with an auto-testbench. The frontend is plain HTML/CSS/JavaScript. The backend is a Java server that computes prime implicants and minimal covers.

## Features

- Truth table and K-Map UI for 2–5 variables.  
- Simplified SOP and POS generation via the backend solver.  
- Verilog module and testbench generation
  
## Project Structure

- `index.html` — Frontend UI and logic (K-Map interaction, solver requests, Verilog generation).  
- `QM_Minimization.java` — Minimization of Boolean Expression using Quine–McCluskey prime implicant method.
- `WebServer.java` — Minimal Java web server exposing POST /solve that returns JSON { sop, pos }.  

## Prerequisites
- Java 17+ (JDK)

## Build and Run

### 1. git clone the repository  
- Clone the repository to a local folder.  
- Example:  
```
git clone https://github.com/DWBH21/Digital_Circuits.git
cd Digital_Circuits
```
### 2. Compile and run WebServer.java
```
javac WebServer.java
java WebServer
```
It will display the following on successful execution
```
Server started on port 8080
Open the HTML file in your browser and press Solve K-Map.
```

### 3. Open index.html  
- Open `index.html` directly in a modern browser (Chrome, Edge, Firefox).  
- "Solve K-Map" will call `http://localhost:8080/solve`.  

## Usage

- Select the number of variables (2–5), click "Generate Table."  
- Set outputs via the truth table dropdowns or click K-Map cells to cycle 0 → 1 → X.  
- Click "Solve K-Map" to compute minimized SOP and POS.  
- Click "Generate Verilog" to view a Verilog module and a compact testbench; use the Copy buttons to copy code.  

## Author

- **Anant Maheshwary**  
- **Roll no - CS24BTECH11006**
