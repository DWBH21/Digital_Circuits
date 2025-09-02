import java.util.*;

class Minterm {
    String s;
    Set<Integer> mintermNos;
    Minterm() {
        s = new String();
        mintermNos = new HashSet<>();
    }
    Minterm(String s, Set<Integer> mintermNos) {
        this.s = s;
        this.mintermNos = mintermNos;
    }
    Minterm(int num, int noVars) {
        StringBuilder sb = new StringBuilder();
        int tempNum = num;
        for (int i = 1; i <= noVars; i++) {
            sb.append(tempNum & 1);
            tempNum >>>= 1;
        }
        this.s = sb.reverse().toString();
        this.mintermNos = new HashSet<>();
        mintermNos.add(num);
    }
    @Override
    public String toString() {
        return s;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((s == null) ? 0 : s.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Minterm other = (Minterm) obj;
        if (s == null) {
            if (other.s != null) return false;
        } else if (!s.equals(other.s)) return false;
        return true;
    }

    
    boolean canMergeWith(Minterm m) {
        if (this.s.length() != m.s.length())
            return false;
        int diff = 0;
        for (int i = 0; i < this.s.length(); i++) {
            if (this.s.charAt(i) == '-' && m.s.charAt(i) != '-')
                return false;
            if (this.s.charAt(i) != '-' && m.s.charAt(i) == '-')
                return false;
            if (this.s.charAt(i) != m.s.charAt(i))
                diff++;
        }
        return (diff == 1);
    }

    Minterm merge(Minterm m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != m.s.charAt(i)) {
                sb.append('-');
            }
            else 
            {
                sb.append(s.charAt(i));
            }
        }
        
        Set<Integer> newMintermNos = new HashSet<>(this.mintermNos);
        newMintermNos.addAll(m.mintermNos);
        return new Minterm(sb.toString(), newMintermNos);
    }

    boolean covers(int mintermNo, int noVars)
    {
        String s = this.s;
        for(int i=noVars-1; i>=0; i--) 
        {
            char p = s.charAt(i);
            int bit = mintermNo & 1;

            mintermNo >>= 1;
            if(p == '-')
                continue;

            p -= '0';
            if(p != bit)   
                return false;
        }
        return true;
    }
}
public class QM_Minimization {
    
    public static List<Minterm> getPrimeImplicants(List<Minterm> minterms) {
        if (minterms.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<Minterm> primeImplicants = new HashSet<>();
        Set<Minterm> mergedMinterms = new HashSet<>();
        boolean[] merged = new boolean[minterms.size()];
        boolean hadMerge = false;

        for (int i = 0; i < minterms.size(); i++) {
            for (int j = i + 1; j < minterms.size(); j++) {
                Minterm m1 = minterms.get(i);
                Minterm m2 = minterms.get(j);
                
                if (m1.canMergeWith(m2)) {
                    Minterm mergedMinterm = m1.merge(m2);
                    mergedMinterms.add(mergedMinterm);
                    merged[i] = true;
                    merged[j] = true;
                    hadMerge = true;
                }
            } 
        }
        
        for (int i = 0; i < minterms.size(); i++) {
            if (!merged[i]) {
                primeImplicants.add(minterms.get(i));
            }
        }
        
        if (!hadMerge)
        {
            return new ArrayList<>(primeImplicants);
        } 
        else
        {
            List<Minterm> nextMintermList = new ArrayList<>(mergedMinterms);
            primeImplicants.addAll(getPrimeImplicants(nextMintermList));
            return new ArrayList<>(primeImplicants);
        }   
    }
    
    static Map<Integer, List<Integer>> buildChart(List<Minterm> primeImplicants, List<Integer> ones, int noVars) 
    {
        Map<Integer, List<Integer>> chart = new LinkedHashMap<>();
        for(int i=0; i<primeImplicants.size(); i++) 
        {
            Minterm m = primeImplicants.get(i);
            List<Integer> covered = new ArrayList<>();
            for(int no : ones)
            {
                if(m.covers(no, noVars))
                    covered.add(no);
            }
            if(!covered.isEmpty())
                chart.put(i, covered);
        }
        return chart;
    }

    static Set<Integer> getMinExpression(List<Minterm> primeImplicants, List<Integer> ones, int noVars) 
    {
        if(primeImplicants.isEmpty())
            return new HashSet<>();

        // Building the prime implicant chart
        Map<Integer, List<Integer>> chart = buildChart(primeImplicants, ones, noVars);
        Set<Integer> remMinterms = new HashSet<>(ones);

        // Selecting esssential prime implicants
        Set<Integer> chosenPIs = reduceByEssentials(chart, remMinterms);

        // If some minterms still remain uncovered, using some trial and error
        // if(!remMinterms.isEmpty())
        // {
        //     Set<Integer> cover = findMinCover(chart, remMinterms);
        //     chosenPIs.addAll(cover);
        // }        
        
        return chosenPIs;
    }

    private static Set<Integer> findMinCover(Map<Integer, List<Integer>> chart, Set<Integer> remMinterms) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findMinCover'");
    }

    static Set<Integer> reduceByEssentials(Map<Integer, List<Integer>> chart, Set<Integer> remMinterms)
    {
        Set<Integer> chosenImplicants = new HashSet<>();
        boolean progress = true;
        while(progress)   // while there is progress
        {
            progress = false;
            
            Map<Integer, List<Integer>> reverseMap = new HashMap<>();   // constructs the map from remTerms to the List of prime implicants it is a part of 
            for(int minterm : remMinterms) 
            {
                reverseMap.put(minterm, new ArrayList<>());
            }
            for(Map.Entry<Integer, List<Integer>> entry : chart.entrySet()) 
            {
                for(int minterm : entry.getValue()) 
                {
                    if(remMinterms.contains(minterm))
                        reverseMap.get(minterm).add(entry.getKey());
                }
            }

            for(Map.Entry<Integer, List<Integer>> entry : reverseMap.entrySet())
            {
                if(entry.getValue().size() == 1) // if the minterm is covered by only one prime implicant
                {
                    int essential = entry.getValue().get(0);
                    if(chosenImplicants.add(essential))
                    {
                        progress = true;
                        remMinterms.removeAll(chart.get(essential));
                    }
                }
            }
        }
        return chosenImplicants;
    }
}