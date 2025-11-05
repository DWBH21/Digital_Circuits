from flask import Flask, request, send_file, jsonify
from flask_cors import CORS
import schemdraw
import schemdraw.logic as logic
from schemdraw.parsing import logicparse
import io
import re


app = Flask(__name__)
CORS(app)

# Converts sop string to schemdraw format
def sop_to_schemdraw_format(sop_string):
    sop_string = sop_string.replace(' ', '')
    terms = sop_string.split('+')
    
    converted_terms = []
    for term in terms:
        literals = []
        i = 0
        while i < len(term):
            var = term[i]
            if i + 1 < len(term) and term[i + 1] == "'":
                literals.append(f"~{var}")
                i += 2
            else:
                literals.append(var)
                i += 1
        converted_terms.append(' & '.join(literals))
    
    return ' | '.join(converted_terms)

# Converts pos string to schemdraw format
def pos_to_schemdraw_format(pos_string):
    expr = pos_string.replace('·', '&').replace('*', '&').replace(' ', '')
    terms = re.findall(r'\(([^)]+)\)', expr)
    
    converted_terms = []
    for term in terms:
        parts = term.split('+')
        literals = []
        
        for part in parts:
            if "'" in part:
                var = part.replace("'", "")
                literals.append(f"~{var}")
            else:
                literals.append(part)
        
        converted_terms.append('(' + ' | '.join(literals) + ')')
    
    return ' & '.join(converted_terms)

# Draws the circuit when the boolean expression evaluates to 0 or 1 for all values of the variables
def draw_constant_circuit(value):
    with schemdraw.Drawing(show=False) as d:
        d += logic.Dot().label(f'F = {value}', loc='right', fontsize=14)
    
    svg = d.get_imagedata('svg').decode()
    svg = svg.replace("<svg ", '<svg style="background-color:white;" ')
    return svg

# Entry point for the draw_sop_circuit request
@app.route('/draw_sop_circuit', methods=['POST'])
def draw_sop_circuit_entry():
    data = request.json
    sop_string = data.get('sop')

    if not sop_string:
        return jsonify({"error": "No 'sop' string provided"}), 400
    
    try:
        sop_clean = sop_string.strip()
        if sop_clean in ['0', '(0)']:
            svg = draw_constant_circuit('0')
            return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')
        elif sop_clean in ['1', '(1)']:
            svg = draw_constant_circuit('1')
            return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')
        
        expr = sop_to_schemdraw_format(sop_string)
        with schemdraw.Drawing(show=False) as d:
            logicparse(expr, outlabel='F')
        
        svg = d.get_imagedata('svg').decode()
        svg = svg.replace("<svg ", '<svg style="background-color:white;" ')
        
        return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# Entry point for the draw_pos_circuit request
@app.route('/draw_pos_circuit', methods=['POST'])
def draw_pos_circuit_entry():
    data = request.json
    pos_string = data.get('pos')

    if not pos_string:
        return jsonify({"error": "No 'pos' string provided"}), 400

    try:
        pos_clean = pos_string.strip()
        if pos_clean in ['0', '(0)']:
            svg = draw_constant_circuit('0')
            return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')
        elif pos_clean in ['1', '(1)']:
            svg = draw_constant_circuit('1')
            return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')
        
        expr = pos_to_schemdraw_format(pos_string)
        with schemdraw.Drawing(show=False) as d:
            logicparse(expr, outlabel='F')
        
        svg = d.get_imagedata('svg').decode()
        svg = svg.replace("<svg ", '<svg style="background-color:white;" ')
        
        return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# Entry point for the draw_waveform request
@app.route('/draw_waveform', methods=['POST'])
def draw_waveform():
    data = request.json
    signal_string = data.get('signals')

    if not signal_string:
        return jsonify({"error": "No 'signals' string provided"}), 400

    try:
        from schemdraw.logic.timing import TimingDiagram
        
        # Parse signals:
        signal_list = []
        for signal in signal_string.split(','):
            if ':' in signal:
                name, wave = signal.split(':', 1)
                wave = wave.replace('?', 'x')
                signal_list.append({'name': name.strip(), 'wave': wave.strip()})
        
        # Create timing diagram
        with schemdraw.Drawing(show=False) as d:
            td = TimingDiagram({'signal': signal_list})
            d += td
        
        svg = d.get_imagedata('svg').decode()
        svg = svg.replace("<svg ", '<svg style="background-color:white;" ')
        
        return send_file(io.BytesIO(svg.encode()), mimetype='image/svg+xml')
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# run on local host:5000
if __name__ == '__main__':
    app.run(debug=True, port=5000)
