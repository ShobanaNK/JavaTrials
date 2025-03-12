import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

class Result {

    /*
     * Complete the 'funnyString' function below.
     *
     * The function is expected to return a STRING.
     * The function accepts STRING s as parameter.
     */

    public static String funnyString(String s) {
        // Difference between each character
        String nDiff = "";
        String rDiff = "";
        char[] nArr = s.toCharArray();
        for (int i=0; i< nArr.length-1; i++) {
            int diff = Math.abs(nArr[i] - nArr[i+1]);
            nDiff = nDiff + diff;
            rDiff = diff + rDiff;
        }
        if (nDiff.equals(rDiff)) {
            return "Funny";
        } else {
            return "Not Funny";
        }
    }

}

public class FunnyString {
    public static void main(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        int q = Integer.parseInt(bufferedReader.readLine().trim());

        IntStream.range(0, q).forEach(qItr -> {
            try {
                String s = bufferedReader.readLine();

                String result = Result.funnyString(s);

                System.out.println(result);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        bufferedReader.close();
    }
}
