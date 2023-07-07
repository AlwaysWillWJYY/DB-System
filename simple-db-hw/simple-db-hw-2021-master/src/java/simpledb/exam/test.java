package simpledb.exam;

import javafx.util.Pair;

import java.util.*;

public class test {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    }
    TreeMap<Integer, List<Integer>>[] row;
    TreeMap<Integer, List<Integer>>[] col;
    int[][] maxn;
    int[][] mat;
    public int dfs(int x,int y) {
        if(maxn[x][y] != 0) return maxn[x][y];
        Integer a = row[x].higherKey(mat[x][y]);
        Integer b = col[y].higherKey(mat[x][y]);
        if(a != null) {
            for (int item : row[x].get(a)) {
                maxn[x][y] = Math.max(maxn[x][y], dfs(x, item));

            }
        }

        if(b != null) {
            for(int item : col[y].get(b)) {
               maxn[x][y] = Math.max(maxn[x][y],dfs(item, y)) ;
            }
        }
        maxn[x][y]++;
        return maxn[x][y];
    }
    public int maxIncreasingCells(int[][] _mat) {
        mat = _mat;
        int m = mat.length;
        int n = mat[0].length;
        maxn = new int[m + 1][n + 1];
        row = new TreeMap[n + 10];
        col = new TreeMap[m + 10];
        for(int i = 0; i < m; i++) {
            for(int j = 0; j < n; j++) {
                int x = mat[i][j];
                if(!row[i].containsKey(x)) {
                    row[i].put(x, new ArrayList<>());
                }
                if(!col[j].containsKey(x)) {
                    col[j].put(x, new ArrayList<>());
                }
                row[i].get(x).add(j);
                col[j].get(x).add(i);
            }
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                dfs(i, j);
            }
        }
        int ans = 0;
        for(int i = 0; i < m; i++) {
            for(int j = 0; j < n; j++) {
                if(ans < maxn[i][j]) ans = maxn[i][j];
            }
        }
        return ans;
    }

}

