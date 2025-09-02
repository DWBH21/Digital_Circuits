
import java.util.*;
public class Minterm {
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
