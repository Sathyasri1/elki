/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.math.linearalgebra;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import elki.logging.Logging;
import elki.math.MathUtil;
import elki.utilities.io.FormatUtil;
import net.jafama.FastMath;

/**
 * Class for systems of linear equations.
 *
 * @author Elke Achtert
 * @since 0.1
 */
public class LinearEquationSystem {
  /**
   * Logger.
   */
  private static final Logging LOG = Logging.getLogger(LinearEquationSystem.class);

  /**
   * A small number to handle numbers near 0 as 0.
   */
  private static final double DELTA = 1E-3;

  /**
   * Indicates trivial pivot search strategy.
   */
  private static final int TRIVAL_PIVOT_SEARCH = 0;

  /**
   * Indicates total pivot search strategy.
   */
  private static final int TOTAL_PIVOT_SEARCH = 1;

  /**
   * Indicates if linear equation system is solvable.
   */
  private boolean solvable;

  /**
   * Indicates if solvability has been checked.
   */
  private boolean solved;

  /**
   * The rank of the coefficient matrix.
   */
  private int rank;

  /**
   * The matrix of coefficients.
   */
  private double[][] coeff;

  /**
   * The right hand side of the equation system.
   */
  private double[] rhs;

  /**
   * Encodes row permutations, row i is at position row[i].
   */
  private int[] row;

  /**
   * Encodes column permutations, column j is at position col[j].
   */
  private int[] col;

  /**
   * Holds the special solution vector.
   */
  private double[] x_0;

  /**
   * Holds the space of solutions of the homogeneous linear equation system.
   */
  private double[][] u;

  /**
   * Indicates if linear equation system is in reduced row echelon form.
   */
  private boolean reducedRowEchelonForm;

  /**
   * Constructs a linear equation system with given coefficient matrix
   * <code>a</code> and right hand side <code>b</code>.
   *
   * @param a the matrix of the coefficients of the linear equation system
   * @param b the right hand side of the linear equation system
   */
  public LinearEquationSystem(double[][] a, double[] b) {
    if(a == null) {
      throw new IllegalArgumentException("Coefficient array is null!");
    }
    if(b == null) {
      throw new IllegalArgumentException("Right hand side is null!");
    }
    if(a.length != b.length) {
      throw new IllegalArgumentException("Coefficient matrix and right hand side " + "differ in row dimensionality!");
    }

    coeff = a;
    rhs = b;
    row = MathUtil.sequence(0, coeff.length);
    col = MathUtil.sequence(0, coeff[0].length);
    rank = 0;
    x_0 = null;
    solved = false;
    solvable = false;
    reducedRowEchelonForm = false;
  }

  /**
   * Constructs a linear equation system with given coefficient matrix
   * <code>a</code> and right hand side <code>b</code>.
   *
   * @param a the matrix of the coefficients of the linear equation system
   * @param b the right hand side of the linear equation system
   * @param rowPermutations the row permutations, row i is at position row[i]
   * @param columnPermutations the column permutations, column i is at position
   *        column[i]
   */
  public LinearEquationSystem(double[][] a, double[] b, int[] rowPermutations, int[] columnPermutations) {
    if(a == null) {
      throw new IllegalArgumentException("Coefficient array is null!");
    }
    if(b == null) {
      throw new IllegalArgumentException("Right hand side is null!");
    }
    if(a.length != b.length) {
      throw new IllegalArgumentException("Coefficient matrix and right hand side " + "differ in row dimensionality!");
    }
    if(rowPermutations.length != a.length) {
      throw new IllegalArgumentException("Coefficient matrix and row permutation array " + "differ in row dimensionality!");
    }
    if(columnPermutations.length != a[0].length) {
      throw new IllegalArgumentException("Coefficient matrix and column permutation array " + "differ in column dimensionality!");
    }

    coeff = a;
    rhs = b;
    this.row = rowPermutations;
    this.col = columnPermutations;
    rank = 0;
    x_0 = null;
    solved = false;
    solvable = false;
    reducedRowEchelonForm = false;
  }

  /**
   * Returns a copy of the coefficient array of this linear equation system.
   *
   * @return a copy of the coefficient array of this linear equation system
   */
  public double[][] getCoefficents() {
    return coeff.clone();
  }

  /**
   * Returns a copy of the right hand side of this linear equation system.
   *
   * @return a copy of the right hand side of this linear equation system
   */
  public double[] getRHS() {
    return rhs.clone();
  }

  /**
   * Returns a copy of the row permutations, row i is at position row[i].
   *
   * @return a copy of the row permutations
   */
  public int[] getRowPermutations() {
    return row.clone();
  }

  /**
   * Returns a copy of the column permutations, column i is at position
   * column[i].
   *
   * @return a copy of the column permutations
   */
  public int[] getColumnPermutations() {
    return col.clone();
  }

  /**
   * Tests if system has already been tested for solvability.
   *
   * @return true if a solution has already been computed, false otherwise.
   */
  public boolean isSolved() {
    return solved;
  }

  /**
   * Solves this linear equation system by total pivot search. "Total pivot
   * search" takes as pivot element the element in the current column having the
   * biggest value. If we have:
   *
   * <pre>
   * ( a_11   ...          a_1n      )
   * (  0     ...          a_2n      )
   * (  0 ... a_ii     ... a_in      )
   * (  0 ... a_(i+1)i ... a_(i+1)n  )
   * (  0 ... a_ni     ... a_nn      )
   * </pre>
   *
   * Then we search for x,y in {i,...n}, so that {@code |a_xy| > |a_ij|}
   */
  public void solveByTotalPivotSearch() {
    solve(TOTAL_PIVOT_SEARCH);
  }

  /**
   * Solves this linear equation system by trivial pivot search. "Trivial pivot
   * search" takes as pivot element the next element in the current column
   * beeing non zero.
   */
  public void solveByTrivialPivotSearch() {
    solve(TRIVAL_PIVOT_SEARCH);
  }

  /**
   * Checks if a solved system is solvable.
   *
   * @return true if this linear equation system is solved and solvable
   */
  public boolean isSolvable() {
    return solvable && solved;
  }

  /**
   * Returns a string representation of this equation system.
   *
   * @param prefix the prefix of each line
   * @param fractionDigits the number of fraction digits for output accuracy
   * @return a string representation of this equation system
   */
  public String equationsToString(String prefix, int fractionDigits) {
    DecimalFormat nf = new DecimalFormat();
    nf.setMinimumFractionDigits(fractionDigits);
    nf.setMaximumFractionDigits(fractionDigits);
    nf.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    nf.setNegativePrefix("");
    nf.setPositivePrefix("");
    return equationsToString(prefix, nf);
  }

  /**
   * Returns a string representation of this equation system.
   *
   * @param prefix the prefix of each line
   * @param nf the number format
   * @return a string representation of this equation system
   */
  public String equationsToString(String prefix, NumberFormat nf) {
    if((coeff == null) || (rhs == null) || (row == null) || (col == null)) {
      throw new NullPointerException();
    }

    int[] coeffDigits = maxIntegerDigits(coeff);
    int rhsDigits = maxIntegerDigits(rhs);

    StringBuilder buffer = new StringBuilder(1000).append(prefix);
    for(int i = 0; i < coeff.length; i++) {
      buffer.append('\n').append(prefix);
      for(int j = 0; j < coeff[row[0]].length; j++) {
        format(nf, buffer, coeff[row[i]][col[j]], coeffDigits[col[j]]) //
            .append(" * x_").append(col[j]);
      }
      format(nf, buffer.append(" ="), rhs[row[i]], rhsDigits);
    }
    return buffer.toString();
  }

  /**
   * Returns a string representation of this equation system.
   *
   * @param nf the number format
   * @return a string representation of this equation system
   */
  public String equationsToString(NumberFormat nf) {
    return equationsToString("", nf);
  }

  /**
   * Returns a string representation of this equation system.
   *
   * @param fractionDigits the number of fraction digits for output accuracy
   * @return a string representation of this equation system
   */
  public String equationsToString(int fractionDigits) {
    return equationsToString("", fractionDigits);
  }

  /**
   * Returns a string representation of the solution of this equation system.
   *
   * @param fractionDigits precision
   *
   * @return a string representation of the solution of this equation system
   */
  public String solutionToString(int fractionDigits) {
    if(!isSolvable()) {
      throw new IllegalStateException("System is not solvable!");
    }

    DecimalFormat nf = new DecimalFormat();
    nf.setMinimumFractionDigits(fractionDigits);
    nf.setMaximumFractionDigits(fractionDigits);
    nf.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
    nf.setNegativePrefix("");
    nf.setPositivePrefix("");

    int row = coeff[0].length >> 1;
    int paramsDigits = integerDigits(u.length);
    int x0Digits = maxIntegerDigits(x_0);
    int[] uDigits = maxIntegerDigits(u);
    StringBuilder buffer = new StringBuilder(1000);
    for(int i = 0; i < x_0.length; i++) {
      format(nf, buffer, x_0[i], x0Digits);
      for(int j = 0; j < u[0].length; j++) {
        if(i == row) {
          buffer.append("  +  a_").append(j).append(" * ");
        }
        else {
          FormatUtil.appendSpace(buffer, paramsDigits + 10);
        }
        format(nf, buffer, u[i][j], uDigits[j]);
      }
      buffer.append('\n');
    }
    return buffer.toString();
  }

  /**
   * Brings this linear equation system into reduced row echelon form with
   * choice of pivot method.
   *
   * @param method the pivot search method to use
   */
  private void reducedRowEchelonForm(int method) {
    final int rows = coeff.length, cols = coeff[0].length;
    int[] pivotPos = new int[2];
    for(int k = 0, m = Math.min(rows, cols); k < m; k++) {
      // pivot search for entry in remaining matrix
      // (depends on chosen method in switch)
      if(method == TRIVAL_PIVOT_SEARCH) {
        nonZeroPivotSearch(k, pivotPos);
      }
      else {
        assert method == TOTAL_PIVOT_SEARCH;
        totalPivotSearch(k, pivotPos);
      }
      double pivot = coeff[this.row[pivotPos[0]]][col[pivotPos[1]]];

      if(LOG.isDebugging()) {
        LOG.debugFine(new StringBuilder(1000) //
            .append("equations ").append(equationsToString(4)) //
            .append("  *** pivot at (").append(pivotPos[0]).append(',').append(pivotPos[1]).append(") = ").append(pivot).append('\n').toString());
      }

      // permute rows and columns to get this entry onto the diagonal
      permutePivot(pivotPos[0], pivotPos[1], k, k);

      // test conditions for exiting loop after this iteration
      // reasons are: Math.abs(pivot) == 0
      if((Math.abs(pivot) <= DELTA)) {
        break;
      }
      rank++;
      pivotOperation(k);
    } // end while

    reducedRowEchelonForm = true;
  }

  /**
   * Method for total pivot search, searches for x,y in {k,...n}, so that
   * {@code |a_xy| > |a_ij|}
   *
   * @param k search starts at entry (k,k)
   * @param pivotpos Output pivot positions
   */
  private void totalPivotSearch(int k, int[] pivotpos) {
    double max = 0;
    int pivotRow = k, pivotCol = k;
    for(int i = k; i < coeff.length; i++) {
      for(int j = k; j < coeff[0].length; j++) {
        // compute absolute value of
        // current entry in absValue
        double absValue = Math.abs(coeff[row[i]][col[j]]);

        // compare absValue with value max
        // found so far
        if(max < absValue) {
          // remember new value and position
          max = absValue;
          pivotRow = i;
          pivotCol = j;
        }
      }
    }
    pivotpos[0] = pivotRow;
    pivotpos[1] = pivotCol;
    return;
  }

  /**
   * Method for trivial pivot search, searches for non-zero entry.
   *
   * @param k search starts at entry (k,k)
   * @param pivotpos Output array
   */
  private void nonZeroPivotSearch(int k, int[] pivotpos) {
    for(int i = k; i < coeff.length; i++) {
      for(int j = k; j < coeff[0].length; j++) {
        // compute absolute value of
        // current entry in absValue
        double absValue = Math.abs(coeff[row[i]][col[j]]);

        // check if absValue is non-zero
        if(absValue > 0) { // found a pivot element
          pivotpos[0] = i;
          pivotpos[1] = j;
          return;
        }
      }
    }
    pivotpos[0] = pivotpos[1] = k;
    return;
  }

  /**
   * permutes two matrix rows and two matrix columns
   *
   * @param r1 the first row for the permutation
   * @param c1 the first column for the permutation
   * @param r2 the second row for the permutation
   * @param c2 the second column for the permutation
   */
  private void permutePivot(int r1, int c1, int r2, int c2) {
    if(r1 != r2) {
      int index = row[r2];
      row[r2] = row[r1];
      row[r1] = index;
    }
    if(c1 != c2) {
      int index = col[c2];
      col[c2] = col[c1];
      col[c1] = index;
    }
  }

  /**
   * performs a pivot operation
   *
   * @param k pivoting takes place below (k,k)
   */
  private void pivotOperation(int k) {
    double pivot = coeff[row[k]][col[k]];

    // pivot row: set pivot to 1
    coeff[row[k]][col[k]] = 1;
    for(int i = k + 1; i < coeff[k].length; i++) {
      coeff[row[k]][col[i]] /= pivot;
    }
    rhs[row[k]] /= pivot;

    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder();
      msg.append("set pivot element to 1 ").append(equationsToString(4));
      LOG.debugFine(msg.toString());
    }

    // for (int i = k + 1; i < coeff.length; i++) {
    for(int i = 0; i < coeff.length; i++) {
      if(i == k) {
        continue;
      }

      // compute factor
      double q = coeff[row[i]][col[k]];

      // modify entry a[i,k], i <> k
      coeff[row[i]][col[k]] = 0;

      // modify entries a[i,j], i > k fixed, j = k+1...n-1
      for(int j = k + 1; j < coeff[0].length; j++) {
        coeff[row[i]][col[j]] = coeff[row[i]][col[j]] - coeff[row[k]][col[j]] * q;
      } // end for j

      // modify right-hand-side
      rhs[row[i]] = rhs[row[i]] - rhs[row[k]] * q;
    } // end for k

    if(LOG.isDebugging()) {
      LOG.debugFine("after pivot operation " + equationsToString(4));
    }
  }

  /**
   * solves linear system with the chosen method
   *
   * @param method the pivot search method
   */
  private void solve(int method) throws NullPointerException {
    // solution exists
    if(solved) {
      return;
    }

    // bring in reduced row echelon form
    if(!reducedRowEchelonForm) {
      reducedRowEchelonForm(method);
    }

    if(!isSolvable(method)) {
      if(LOG.isDebugging()) {
        LOG.debugFine("Equation system is not solvable!");
      }
      return;
    }

    // compute one special solution
    final int cols = coeff[0].length;
    int numbound = 0, numfree = 0;
    int[] boundIndices = new int[cols], freeIndices = new int[cols];
    x_0 = new double[cols];
    outer: for(int i = 0; i < coeff.length; i++) {
      for(int j = i; j < coeff[row[i]].length; j++) {
        if(coeff[row[i]][col[j]] == 1) {
          x_0[col[i]] = rhs[row[i]];
          boundIndices[numbound++] = col[i];
          continue outer;
        }
      }
      freeIndices[numfree++] = i;
    }

    StringBuilder msg = LOG.isDebuggingFine() ? new StringBuilder() : null;
    if(msg != null) {
      msg.append("\nSpecial solution x_0 = [") //
          .append(FormatUtil.format(x_0, ",", FormatUtil.NF4)).append(']') //
          .append("\nbound Indices ").append(FormatUtil.format(boundIndices, ",")) //
          .append("\nfree Indices ").append(FormatUtil.format(freeIndices, ","));
    }

    // compute solution space of homogeneous linear equation system
    Arrays.sort(boundIndices, 0, numbound);
    int freeIndex = 0;
    int boundIndex = 0;
    u = new double[cols][numfree];

    for(int j = 0; j < u[0].length; j++) {
      for(int i = 0; i < u.length; i++) {
        if(freeIndex < numfree && i == freeIndices[freeIndex]) {
          u[i][j] = 1;
        }
        else if(boundIndex < numbound && i == boundIndices[boundIndex]) {
          u[i][j] = -coeff[row[boundIndex]][freeIndices[freeIndex]];
          boundIndex++;
        }
      }
      freeIndex++;
      boundIndex = 0; // Restart
    }

    if(msg != null) {
      msg.append("\nU");
      for(double[] anU : u) {
        msg.append('\n').append(FormatUtil.format(anU, ",", FormatUtil.NF4));
      }
      LOG.debugFine(msg.toString());
    }

    solved = true;
  }

  /**
   * Checks solvability of this linear equation system with the chosen method.
   *
   * @param method the pivot search method
   * @return true if linear system in solvable
   */
  private boolean isSolvable(int method) throws NullPointerException {
    if(solved) {
      return solvable;
    }

    if(!reducedRowEchelonForm) {
      reducedRowEchelonForm(method);
    }

    // test if rank(coeff) == rank(coeff|rhs)
    for(int i = rank; i < rhs.length; i++) {
      if(Math.abs(rhs[row[i]]) > DELTA) {
        solvable = false;
        return false; // not solvable
      }
    }

    solvable = true;
    return true;
  }

  /**
   * Returns the maximum integer digits in each column of the specified values.
   *
   * @param values the values array
   * @return the maximum integer digits in each column of the specified values
   */
  private int[] maxIntegerDigits(double[][] values) {
    int[] digits = new int[values[0].length];
    for(int j = 0; j < values[0].length; j++) {
      for(double[] value : values) {
        digits[j] = Math.max(digits[j], integerDigits(value[j]));
      }
    }
    return digits;
  }

  /**
   * Returns the maximum integer digits of the specified values.
   *
   * @param values the values array
   * @return the maximum integer digits of the specified values
   */
  private int maxIntegerDigits(double[] values) {
    double val = 0;
    for(double value : values) {
      val = Math.max(val, Math.abs(value));
    }
    return integerDigits(val);
  }

  /**
   * Returns the integer digits of the specified double value.
   *
   * @param d the double value
   * @return the integer digits of the specified double value
   */
  private int integerDigits(double d) {
    double value = Math.abs(d);
    return value < 10 ? 1 : (int) FastMath.log10(value) + 1;
  }

  /**
   * Helper method for output of equations and solution. Appends the specified
   * double value to the given string buffer according the number format and the
   * maximum number of integer digits.
   *
   * @param nf the number format
   * @param buffer the string buffer to append the value to
   * @param value the value to append
   * @param maxIntegerDigits the maximum number of integer digits
   * @return the buffer for chaining
   */
  private StringBuilder format(NumberFormat nf, StringBuilder buffer, double value, int maxIntegerDigits) {
    return FormatUtil.appendSpace(buffer.append(value >= 0 ? " + " : " - "), //
        maxIntegerDigits - integerDigits(value))//
        .append(nf.format(Math.abs(value)));
  }

  /**
   * Return dimensionality of spanned subspace.
   *
   * @return dim
   */
  public int subspacedim() {
    return coeff[0].length - coeff.length;
  }
}
