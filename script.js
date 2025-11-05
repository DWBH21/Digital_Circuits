document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('num-variables').value = 4;
    generateTable();
});

const numVar = document.getElementById('num-variables');
const tTable = document.getElementById('truth-table');
const kmapDiv = document.getElementById('kmap-container');
const sopOut = document.getElementById('sop-equation');
const posOut = document.getElementById('pos-equation');
const mainDiv = document.getElementById('main-content');
let n = 4;
let outputs = [];
let cellsByIndex = new Map(); // Direct mapping from minterm index to cell element
const grayMap = {'00': 0, '01': 1, '11': 3, '10': 2, '0': 0, '1': 1};

function generateTable() {
    n = parseInt(numVar.value);
    mainDiv.classList.remove('hidden');
    const numRows = Math.pow(2, n);
    outputs = Array(numRows).fill('0');
    cellsByIndex.clear(); // Clear the mapping

    let header = '<tr><th>#</th>';
    for (let i = 0; i < n; i++) {
        header += `<th>${String.fromCharCode(65 + i)}</th>`;
    }
    header += `<th>F</th></tr>`;
    tTable.querySelector('thead tr').innerHTML = header;

    let body = '';
    for (let i = 0; i < numRows; i++) {
        const binary = i.toString(2).padStart(n, '0');
        body += `<tr><td>${i}</td>`;
        for (let j = 0; j < n; j++) {
            body += `<td>${binary[j]}</td>`;
        }
        
        // Added a unique ID to the select element
        body += `<td><select id="output-tt-${i}" onchange="updateOutput(${i}, this.value)">
            <option value="0">0</option>
            <option value="1">1</option>
            <option value="X">X</option>
        </select></td></tr>`;
    }
    tTable.querySelector('tbody').innerHTML = body;
    generateKmapGrid();
}

function updateOutput(index, value) {
    outputs[index] = value;
    generateKmapGrid();
}

function updateAllOutputs(value) {
    outputs.fill(value);
    document.querySelectorAll('#truth-table tbody select').forEach(select => {
        select.value = value;
    });
    generateKmapGrid();
}

function generateKmapGrid() {
    kmapDiv.innerHTML = '';
    cellsByIndex.clear(); // Clear the mapping
    let rows = 4, cols = 4;
    let rowLabels = ['00', '01', '11', '10'], colLabels = ['00', '01', '11', '10'];

    if (n === 2) {
        rows = 2; cols = 2;
        rowLabels = ['0', '1'];
        colLabels = ['0', '1'];
    } else if (n === 3) {
        rows = 2; cols = 4;
        rowLabels = ['0', '1'];
    }

    const getVarLabel = (vars, value) => {
        return vars.split('').map((char, i) => 
            value[i] === '0' ? `${char}'` : char
        ).join('');
    };

    const createKmap = (offset, aVar) => {
        const gridContainer = document.createElement('div');
        gridContainer.className = 'kmap-grid-container-inner';
        
        if (n === 5) {
            gridContainer.innerHTML = `<h3 style="margin-bottom: 0;">${aVar}</h3>`;
        }

        const grid = document.createElement('div');
        grid.className = 'kmap-grid';
        grid.style.gridTemplateColumns = `repeat(${cols + 1}, minmax(0, 1fr))`;
        grid.style.gridTemplateRows = `repeat(${rows + 1}, minmax(0, 1fr))`;

        const varMap = {2: ['A', 'B'], 3: ['A', 'BC'], 4: ['AB', 'CD'], 5: ['BC', 'DE']};
        const [rowVars, colVars] = varMap[n];

        grid.innerHTML = '<div class="kmap-cell-header"></div>';

        colLabels.forEach(col => {
            grid.innerHTML += `<div class="kmap-cell-header">${getVarLabel(colVars, col)}</div>`;
        });

        for (let r = 0; r < rows; r++) {
            grid.innerHTML += `<div class="kmap-cell-header">${getVarLabel(rowVars, rowLabels[r])}</div>`;
            for (let c = 0; c < cols; c++) {
                let index;
                if (n === 2) index = (r << 1) | c;
                else if (n === 3) index = (r << 2) | grayMap[colLabels[c]];
                else if (n === 4) index = (grayMap[rowLabels[r]] << 2) | grayMap[colLabels[c]];
                else if (n === 5) index = offset + ((grayMap[rowLabels[r]] << 2) | grayMap[colLabels[c]]);

                const cell = document.createElement('div');
                cell.className = 'kmap-cell';
                cell.setAttribute('onclick', `cycleKmapCell(${index})`);
                cell.innerHTML = `
                    <span class="kmap-cell-minterm">${index}</span>
                    <span class="kmap-cell-value ${outputs[index] === 'X' ? 'is-x' : ''}">${outputs[index]}</span>
                `;
                
                // Store direct reference in our map
                cellsByIndex.set(index, cell);
                
                grid.appendChild(cell);
            }
        }
        gridContainer.appendChild(grid);
        return gridContainer;
    };

    if (n === 5) {
        kmapDiv.className = 'kmap-5var';
        kmapDiv.appendChild(createKmap(0, 'A=0'));
        kmapDiv.appendChild(createKmap(16, 'A=1'));
    } else {
        kmapDiv.className = 'kmap-grid-container';
        kmapDiv.appendChild(createKmap(0));
    }
}

function cycleKmapCell(mintermIndex) {
    const currentValue = outputs[mintermIndex];
    let nextValue;
    if (currentValue === '0') {
        nextValue = '1';
    } else if (currentValue === '1') {
        nextValue = 'X';
    } else {
        nextValue = '0';
    }
    
    outputs[mintermIndex] = nextValue;
    
    // Use the new ID to find the select element directly
    const selectElement = document.getElementById(`output-tt-${mintermIndex}`);
    if (selectElement) {
        selectElement.value = nextValue;
    }
    
    generateKmapGrid();
}

async function solveKmap() {
    sopOut.innerHTML = '<span style="color: #6b7280;">Solving...</span>';
    posOut.innerHTML = '<span style="color: #6b7280;">Solving...</span>'; // Added this for consistency

    // Gather the on-set and don't-care minterms from the UI
    const onSet = [];
    const dontCares = [];
    for (let i = 0; i < outputs.length; i++) {
        if (outputs[i] === '1') {
            onSet.push(i);
        } else if (outputs[i] === 'X') {
            dontCares.push(i);
        }
    }

    // Prepare the data to send to the Java backend
    const requestData = {
        noVars: n,
        onSet: onSet,
        dontCares: dontCares
    };

    try {
        // Send the data to the server and wait for the response
        const response = await fetch('http://localhost:8080/solve', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(requestData),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();

        // Update the UI with the solution
        sopOut.textContent = result.sop || '1';
        posOut.textContent = result.pos || '1';
        
        // 1. Fetch the circuit diagram from the Python server
        fetchAndDisplaySOPCircuit(result.sop);
        fetchAndDisplayPOSCircuit(result.pos);

        // 2. Get waveform data string and fetch the diagram
        const waveformString = getWaveformString();
        fetchAndDisplayWaveform(waveformString);
        
    } catch (error) {
        sopOut.innerHTML = `<span style="color: #ef4444;">Error: Could not connect to a server. Are the Java AND Python servers running?</span>`;
        posOut.innerHTML = `<span style="color: #ef4444;">Error: ${error.message}</span>`;
        console.error('Error solving K-map:', error);
    }
}

function generateVerilog() {
    const sop = sopOut.textContent;
    if (!sop || sop.startsWith('Error') || sop.startsWith('Solve')) {
        alert("Please solve the K-map first to get a valid SOP expression.");
        return;
    }

    document.getElementById('verilog-output').classList.remove('hidden');
    const varNames = Array.from({length: n}, (_, i) => String.fromCharCode(65 + i));
    const inputs = varNames.join(', ');

    // Verilog Module
    let sopForVerilog;

    if (sop === '1') {
        sopForVerilog = "1'b1";
    } else if (sop === '0') {
        sopForVerilog = "1'b0";
    } else {
        const parseTerm = (term) => {
            let literals = [];
            for (let i = 0; i < term.length; i++) {
                const char = term[i];
                if (i + 1 < term.length && term[i+1] === "'") {
                    literals.push(`(~${char})`);
                    i++; // Skip the apostrophe
                } else {
                    literals.push(char);
                }
            }
            if (literals.length > 1) {
                return `(${literals.join(' & ')})`;
            }
            return literals.join(' & '); // Handles single literal terms
        };

        const terms = sop.split(' + ');
        const verilogTerms = terms.map(parseTerm);
        sopForVerilog = verilogTerms.join(' | ');
    }

    const verilogModule = `module boolean_expression(\n  output F,\n  input  ${inputs}\n);\n\n  assign F = ${sopForVerilog};\n\nendmodule`;
    document.getElementById('verilog-code').textContent = verilogModule;

    // Verilog Testbench
    const regs = varNames.map(v => `reg ${v};`).join('\n  ');
    const wires = 'wire F;';
    const dut = `boolean_expression dut(F, ${inputs});`;
    
    const reversedVarNames = [...varNames].reverse().join(', ');
    const varVector = `{${reversedVarNames}}`;
    const numCombinations = Math.pow(2, n);

    let initialBlock = `  integer i;\n  initial begin\n`;
    initialBlock += `    for (i = 0; i < ${numCombinations}; i = i + 1) begin\n`;
    initialBlock += `      ${varVector} = i;\n`;
    initialBlock += `      #10;\n`;
    initialBlock += `    end\n`;
    initialBlock += `    $finish;\n  end`;

    const monitorFormat = varNames.map(v => `${v}=%b`).join(', ');
    const monitorVars = varNames.join(', ');
    const monitor = `  initial begin\n    $monitor("${monitorFormat}, \tF=%b", ${monitorVars}, F);\n  end`;

    const testbench = `module testbench;\n\n  ${regs}\n  ${wires}\n\n  ${dut}\n\n${initialBlock}\n\n${monitor}\n\nendmodule`;
    document.getElementById('testbench-code').textContent = testbench;
}

function copyToClipboard(event, elementId) {
    const code = document.getElementById(elementId).textContent;
    navigator.clipboard.writeText(code).then(() => {
        const button = event.target;
        button.textContent = 'Copied!';
        setTimeout(() => { button.textContent = 'Copy'; }, 2000);
    }, (err) => {
        console.error('Could not copy text: ', err);
    });
}

// Fetches the circuit diagram from the Python server.
async function fetchAndDisplaySOPCircuit(sopString) {
    const imgElement = document.getElementById('sop-circuit-image');
    imgElement.src = ''; // Clear old image
    imgElement.alt = 'Loading SOP circuit diagram...';

    if (imgElement.src && imgElement.src.startsWith('blob:')) {
        URL.revokeObjectURL(imgElement.src);
    }

    try {
        console.log(sopString);
        const response = await fetch('http://localhost:5000/draw_sop_circuit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sop: sopString })
        });

        if (!response.ok) {
            throw new Error(`Circuit drawer failed with status: ${response.status}`);
        }

        const imageBlob = await response.blob();
        const imageUrl = URL.createObjectURL(imageBlob);
        imgElement.src = imageUrl;
        imgElement.alt = 'SOP Circuit Diagram';
    } catch (error) {
        console.error('Error drawing SOP circuit:', error);
        imgElement.alt = 'Error loading SOP circuit diagram. Is the Python server running?';
    }
}

async function fetchAndDisplayPOSCircuit(posString) {
    const imgElement = document.getElementById('pos-circuit-image');
    imgElement.src = ''; // Clear old image
    imgElement.alt = 'Loading POS circuit diagram...';

    if (imgElement.src && imgElement.src.startsWith('blob:')) {
        URL.revokeObjectURL(imgElement.src);
    }

    try {
        console.log(posString);
        const response = await fetch('http://localhost:5000/draw_pos_circuit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pos: posString })
        });

        if (!response.ok) {
            throw new Error(`Circuit drawer failed with status: ${response.status}`);
        }

        const imageBlob = await response.blob();
        const imageUrl = URL.createObjectURL(imageBlob);
        imgElement.src = imageUrl;
        imgElement.alt = 'POS Circuit Diagram';
    } catch (error) {
        console.error('Error drawing POS circuit:', error);
        imgElement.alt = 'Error loading POS circuit diagram. Is the Python server running?';
    }
}

// Fetches the waveform diagram from the Python server.
async function fetchAndDisplayWaveform(signalData) {
    const imgElement = document.getElementById('waveform-image');
    
    if (imgElement.src && imgElement.src.startsWith('blob:')) {
        URL.revokeObjectURL(imgElement.src);
    }
    
    imgElement.src = ''; // Clear old image
    imgElement.alt = 'Loading waveform diagram...';

    
    try {
        console.log(signalData);
        const response = await fetch('http://localhost:5000/draw_waveform', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ signals: signalData })
        });

        if (!response.ok) {
            throw new Error(`Waveform drawer failed with status: ${response.status}`);
        }

        const imageBlob = await response.blob();
        const imageUrl = URL.createObjectURL(imageBlob);
        imgElement.src = imageUrl;
        imgElement.alt = 'Waveform Diagram';
    } catch (error) {
        console.error('Error drawing waveform:', error);
        imgElement.alt = 'Error loading waveform diagram. Is the Python server running?';
    }
}

// Gather data from the truth table and format it for schemdraw.
function getWaveformString() {
    const numVars = parseInt(document.getElementById('num-variables').value);
    const varNames = ['A', 'B', 'C', 'D', 'E'].slice(0, numVars);
    const numRows = Math.pow(2, numVars);
    
    let signals = {};
    varNames.forEach(name => signals[name] = '');
    signals['F'] = '';

    for (let i = 0; i < numRows; i++) {
        for (let j = 0; j < numVars; j++) {
            const varName = varNames[j];
            const bit = (i >> (numVars - 1 - j)) & 1;
            signals[varName] += bit.toString();
        }
    }

    for (let i = 0; i < numRows; i++) {
        const outputSelect = document.getElementById(`output-tt-${i}`); 
        let val = '0';
        if (outputSelect) {
            val = outputSelect.value;
        }
        if (val === 'X') val = '?';
        signals['F'] += val;
    }

    return Object.entries(signals)
                 .map(([name, wave]) => `${name}: ${wave}`)
                 .join(', ');
}
