import org.jooq.impl.QOM;

import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import static java.lang.System.exit;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class MagicSquare {
    public static void main(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        List<List<Integer>> s = new ArrayList<>();

        IntStream.range(0, 3).forEach(i -> {
            try {
                s.add(
                        Stream.of(bufferedReader.readLine().replaceAll("\\s+$", "").split(" "))
                                .map(Integer::parseInt)
                                .collect(toList())
                );
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        int result = formingMagicSquare(s);
        System.out.println(result);

        bufferedReader.close();
    }

    /*
     * Complete the 'formingMagicSquare' function below.
     *
     * The function is expected to return an INTEGER.
     * The function accepts 2D_INTEGER_ARRAY s as parameter.
     */

    //static List<Integer> notPresent = new ArrayList<>();
    static HashMap<Integer, List<String>> locations = new HashMap<>();
    static List<Integer> rowDiff = new ArrayList<>();
    static List<Integer> colDiff = new ArrayList<>();
    static int dia1Diff = 0;
    static int dia2Diff = 0;
    public static int formingMagicSquare(List<List<Integer>> s) {
        // Write your code here
        //System.out.println(s);

        // Find the magic constant
        int sum = 0;
        int n = s.size();
        for (int i = 1; i <= (n*n); i++) {
            sum += i;
        }
        int mConstant = sum / n;

        //System.out.println(mConstant);

        rowDiff = s.stream().map(e -> getMagicDiff(e, mConstant)).collect(toList());
        //System.out.println(rowDiff);

        List<List<Integer>> trans = new ArrayList<>();
        for (List<Integer> l: s) {
            trans.add(new ArrayList<>(l));
        }
        // matrix transpose
        for (int i=0; i<n; i++) {
            for (int j= i+1; j<n; j++) {
                int tmp = trans.get(i).get(j);
                trans.get(i).set(j,trans.get(j).get(i));
                trans.get(j).set(i, tmp);
            }
        }

        //System.out.println(trans);
        //System.out.println(s);

        colDiff = trans.stream().map(e -> getMagicDiff(e, mConstant)).collect(toList());
        //System.out.println(colDiff);

        int i = 0;
        int dia1sum = 0;
        int dia2sum = 0;
        while (i<n) {
            dia1sum += s.get(i).get(i);
            dia2sum += s.get(i).get(n-i-1);
            i++;
        }

        dia1Diff = dia1sum - mConstant;
        dia2Diff = dia2sum - mConstant;

        // Populate the location Map
        for (int k = 0; k < n; k++) {
            for(int l = 0; l < n; l++) {
                int num = s.get(k).get(l);
                if(!locations.containsKey(num)) {
                    locations.put(num, new ArrayList<>());
                }
                locations.get(num).add(k + "," + l);
            }
        }

        for (int k = 1; k<=n*n ; k++) {
            if(!locations.containsKey(k)) {
                locations.put(k, new ArrayList<>());
            }
        }

        /*System.out.println(dia1Diff);
        System.out.println(dia2Diff);

        System.out.println(locations);*/

        Integer MinReplaceCnt = null;
        for (int k=0; k<n; k++) {
            for (int l=0; l<n; l++) {
                List<List<Integer>> newS = new ArrayList<>();
                for (List<Integer> e: s) {
                    newS.add(new ArrayList<>(e));
                }
                Integer res = replaceAndUpdate(k, l, newS, n);
                if (res!= null) {
                    if (MinReplaceCnt == null || res < MinReplaceCnt ) {
                        MinReplaceCnt = res;
                    }
                }
            }
        }

        return MinReplaceCnt;
    }

    private static Integer getMagicDiff(List<Integer> l, int n) {
        int sum = l.stream().reduce(0, Integer::sum);
        return sum - n;
    }


    private static Integer replaceAndUpdate(int k, int l, List<List<Integer>> s, int n) {
        Integer replacCnt = 0;
        int number = s.get(k).get(l);
        List<String> positions = locations.get(number);
        List<Integer> notPresent = locations.keySet().stream().filter(e -> locations.get(e).size() == 0).collect(toList());
        //System.out.println(notPresent);
        if(positions.size() > 1) {
            // replace with not present.

            int rd = rowDiff.get(k);
            int cd = colDiff.get(l);
            List<Integer> m = notPresent.stream().filter(e ->((e - number) + rd ) == 0 && ((e - number) + cd ) == 0).collect(toList());
            int newNum = 0;
            int newK = 0;
            int newL = 0;
            String newPos = null;
            if (m.size() == 1) {
                newNum = m.get(0);
            } else if (rd == cd) {
                newNum = number - rd;
                List<String> pos = locations.get(newNum);
                newPos = pos.get(0);

            } else { // aadd more cases here
                System.out.println("Error---");
                exit(1);
            }
            s.get(k).set(l,newNum);
            locations.get(newNum).add(k + "," + l);
            //System.out.println("set");
            //System.out.println(s);
            positions.remove(k + "," + l);
            rowDiff.set(k, rd + (newNum - number));
            colDiff.set(l, cd + (newNum - number));
            if (k == l) {
                dia1Diff = dia1Diff + (newNum - number);
            }

            if (k == (n-l-1)) {
                dia2Diff = dia2Diff + (newNum - number);
            }
            replacCnt += (Math.abs(newNum - number));

            if(rowDiff.stream().allMatch(num -> num == 0) &&
                colDiff.stream().allMatch(num -> num == 0) &&
                dia1Diff == 0 && dia2Diff == 0) {
                return replacCnt;
            }



            if (newPos ==  null) {
                List<Integer> MulPresent = locations.keySet().stream().filter(e -> locations.get(e).size() > 1).collect(toList());
                if (MulPresent.size() > 0) {
                    newPos = locations.get(MulPresent.get(0)).get(0);
                }
            }

            if(newPos != null) {
                //System.out.println(newPos);
                String[] newPositions = newPos.split(",");
                newK = Integer.parseInt(newPositions[0]);
                newL = Integer.parseInt(newPositions[1]);
                Integer res = replaceAndUpdate(newK, newL, s, n);
                if (res != null) {
                    replacCnt += res;
                }

            } else {
                System.out.println("cannot continue... backtracking");
                return null;
            }
        }
        if(replacCnt == 0) {
            return null;
        } else {
            return replacCnt;
        }
    }
}
