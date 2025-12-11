# Karnaugh Map Solver (Frontend + Java Backend + Python Backend)

An interactive web tool to create truth tables, visualize Karnaugh maps (2–5 variables), minimize boolean expressions (SOP and POS), draw Digital Circuit Diagram, generate waveform and generate Verilog code with an auto-testbench. The frontend is plain HTML/CSS/JavaScript. The backend is a Java server that computes prime implicants and minimal covers and a python application using Flask that generates circuit diagrams and waveforms. 

## Features

- Truth table and K-Map UI for 2–5 variables.  
- Simplified SOP and POS generation via the backend solver. 
- Digital Circuit Diagram for SOP and POS
- Verilog module and testbench generation
- Waveform Generation
  
## Project Structure

- `index.html` — Frontend UI and logic (K-Map interaction, solver requests, Verilog generation).  
- `QM_Minimization.java` — Minimization of Boolean Expression using Quine–McCluskey prime implicant method.
- `WebServer.java` — Minimal Java web server exposing POST /solve that returns JSON { sop, pos }.  
- `app.py` - Python Backend for building Digital Circuit Diagram and Waveform Generation using Flask

## Prerequisites
- Java 17+ (JDK)
- Python3 (with pip so that flask can be installed) 

## Build and Run

### 1. git clone the repository  
- Clone the repository to a local folder.  
- Example:  
```
git clone https://github.com/DWBH21/Digital_Circuits.git
cd Digital_Circuits
```
### 2. Compile and run WebServer.java - (Starts the Java Backend)
- Open a new terminal and run the following commands: 
```
javac WebServer.java
java WebServer
```
It will display the following on successful execution
```
Server started on port 8080
```

### 3. Run app.py - (Starts the Python Backend)
- Open a new terminal and the run the following command (keep the java terminal running):
```
python3 app.py
```
 - If you get an error: No module named 'flask', then you install it using the following commands (in a virtual environment) 
``` 
python3 -m venv venv
source venv/bin/activate
pip install flask
```
and then run (in the same terminal) 
```
python3 app.py
```
It will display the following on successful execution 
```
Running on http://127.0.0.1:5000
```

### 4. Open index.html  
- Open `index.html` directly in a modern browser (Chrome, Edge, Firefox).  
- "Solve K-Map" will call `http://localhost:8080/solve`.  

## Usage

- Select the number of variables (2–5), click "Generate Table."  
- Set outputs via the truth table dropdowns or click K-Map cells to cycle 0 → 1 → X.  
- Click "Solve K-Map" to compute minimized SOP and POS. 
- This will also show the Digital Circuit Diagram for both SOP and POS and the Waveform Diagram
- Click "Generate Verilog" to view a Verilog module and a compact testbench; use the Copy buttons to copy code.  

## Author

- **Anant Maheshwary**  
- **Roll no - CS24BTECH11006**
