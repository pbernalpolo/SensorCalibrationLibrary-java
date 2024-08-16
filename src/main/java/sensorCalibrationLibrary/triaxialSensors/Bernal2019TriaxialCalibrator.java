package sensorCalibrationLibrary.triaxialSensors;



public class Bernal2019TriaxialCalibrator
{
    ////////////////////////////////////////////////////////////////
    // PARAMETERS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Iterative calibration method will stop if solution does not improve after {@link #MAX_CALIBRATION_ITERATIONS_WITHOUT_IMPROVEMENT} iterations.
     */
    private static final int MAX_CALIBRATION_ITERATIONS_WITHOUT_IMPROVEMENT = 20;
    
    /**
     * Iterative calibration method will stop if not converged after {@link #MAX_CALIBRATION_ITERATIONS} iterations.
     */
    private static final int MAX_CALIBRATION_ITERATIONS = 1000;

    
    
    ////////////////////////////////////////////////////////////////
    // PRIVATE VARIABLES
    ////////////////////////////////////////////////////////////////
    // order of the polynomial used for the temperature dependence
    private int N;
    private int maxN21;  // 2*N+1
    private int maxN41;  // 4*N+1
    // sum of weights
    private double W;
    // tensors built with measurements (their size depend on N)
    private double[][][][][] X4;  // \sum_m w_m x_{m i} x_{m j} x_{m k} x_{m l} T_m^n (Nx4x4x4x4)
    private double[][][] Y2;  // \sum_m w_m x_{m i} x_{m j} y_m^2 T_m^n (Nx4x4)
    // auxiliary tensors used to build the previous ones
    private double[] x1;
    private double[][] x2;
    private double[][][] x3;
    private double[][][][] x4;
    private double[] y1;
    private double[][] y2;
    private double[] Tn;
    // current approximation to the solution
    private double[] zk;  // z_k = ( (K11,K21,K22,K31,K32,K33,c1,c2,c3)^(0) ,
                          // (K11,K21,K22,K31,K32,K33,c1,c2,c3)^(1) , ... ,
                          // (K11,K21,K22,K31,K32,K33,c1,c2,c3)^(N) )_k (9*Nx1)
    // optimal approximation to the solution
    private double[] z;  // z = ( (K11,K21,K22,K31,K32,K33,c1,c2,c3)^(0) ,
                         // (K11,K21,K22,K31,K32,K33,c1,c2,c3)^(1) , ... ,
                         // (K11,K21,K22,K31,K32,K33,c1,c2,c3)^(N) )_optimal (9*Nx1)
    // coefficients of the polynomial matrix A: A^(n)
    private double[][][] A;
    
    
    
    ////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
    
    public Bernal2019TriaxialCalibration getCalibration()
    {
        Bernal2019TriaxialCalibration output = new Bernal2019TriaxialCalibration();
        output.set( this.z );
        return output;
    }


    public void setPolynomialOrder( int polynomialOrder )
    {
        this.N = polynomialOrder;
        this.maxN21 = 2 * polynomialOrder + 1;
        this.maxN41 = 4 * polynomialOrder + 1;
        // calibration tensors
        this.X4 = new double[this.maxN41][4][4][4][4];
        this.Y2 = new double[this.maxN21][4][4];
        // auxiliary tensors
        this.x1 = new double[4];
        this.x1[3] = 1.0;
        this.x2 = new double[4][4];
        this.x3 = new double[4][4][4];
        this.x4 = new double[4][4][4][4];
        this.y1 = new double[4];
        this.y2 = new double[4][4];
        this.Tn = new double[this.maxN41];
        // algorithm variables
        this.zk = new double[9 * ( polynomialOrder + 1 )];
        this.z = new double[9 * ( polynomialOrder + 1 )];
        this.A = new double[polynomialOrder + 1][3][4];
        // now we reset
        this.reset_tensors();
    }


    // updates the tensors with a data combination
    public void addCalibrationData( double w , double x1 , double x2 , double x3 , double T , double y )
    {
        this.x1[0] = x1;
        this.x1[1] = x2;
        this.x1[2] = x3;
        // we define the auxiliary factor for this measurement
        double alpha = w / ( this.W + w );
        // and we add the contribution to the sum of weights
        this.W += w;
        // we define the square of the module
        double y0 = y * y;
        // now we add the contribution of this measurement to the tensors
        // zeroth-order tensors
        this.Tn[0] = 1.0;
        for( int n = 1; n < this.maxN41; n++ )
            this.Tn[n] = this.Tn[n - 1] * T;
        // first-order tensors
        for( int i = 0; i < 4; i++ ) {
            this.y1[i] = y0 * this.x1[i];
            // second-order tensors
            for( int j = 0; j < 4; j++ ) {
                this.x2[i][j] = this.x1[i] * this.x1[j];
                this.y2[i][j] = this.y1[i] * this.x1[j];
                for( int n = 0; n < this.maxN21; n++ )
                    this.Y2[n][i][j] = ( 1.0 - alpha ) * this.Y2[n][i][j] + alpha * this.y2[i][j] * this.Tn[n];
                // third-order tensor
                for( int k = 0; k < 4; k++ ) {
                    this.x3[i][j][k] = this.x2[i][j] * this.x1[k];
                    // fourth-order tensor
                    for( int l = 0; l < 4; l++ ) {
                        this.x4[i][j][k][l] = this.x3[i][j][k] * this.x1[l];
                        for( int n = 0; n < this.maxN41; n++ )
                            this.X4[n][i][j][k][l] = ( 1.0 - alpha ) * this.X4[n][i][j][k][l]
                                    + alpha * this.x4[i][j][k][l] * this.Tn[n];
                    }  // end l
                }  // end k
            }  // end j
        }  // end i
    }  // end include_measurementCalibration( double w , double[] x , double y , double T )


    // computes the temperature calibration of theN order. First we need to set the
    // tensors with the two methods above
    public void calibrate()
    {
        // we compute the solution using the Levenberg–Marquardt algorithm
        this.reset_zk();
        double minError = Double.MAX_VALUE;
        int itWithoutImprovement = 0;
        for( int k = 0; k < MAX_CALIBRATION_ITERATIONS; k++ ) {
            // first of all we update the calibration matrix
            this.update_A();
            // we compute the vector J^T*W*dy
            double[] JTWdy = this.get_JTWdy();
            // we compute the J^T*W*J matrix
            double[][] JTWJ = this.get_JTWJ();
            // we compute the next delta in the solution approximation ( delta^T*(J^T*J) = [J^T*(y-f)]^T )
            this.solve( JTWJ , JTWdy , JTWJ.length );  // now dz is stored in JTWdy
            // we compute the current error in the search for the zeros
            double err = 0.0;
            for( int i = 0; i < 9 * ( this.N + 1 ); i++ ) {
                err += JTWdy[i] * JTWdy[i];
            }
            if( err < minError ) {
                for( int i = 0; i < 9 * ( this.N + 1 ); i++ )
                    this.z[i] = this.zk[i];
                minError = err;
                itWithoutImprovement = 0;
            } else {
                itWithoutImprovement++;
                if( itWithoutImprovement > MAX_CALIBRATION_ITERATIONS_WITHOUT_IMPROVEMENT )
                    break;
            }
            // we update the solution
            for( int i = 0; i < 9 * ( this.N + 1 ); i++ )
                this.zk[i] += JTWdy[i];
            // and now we correct for a right-handed orientation
            if( this.zk[0] < 0.0 )
                this.zk[0] = -this.zk[0];
            if( this.zk[2] < 0.0 )
                this.zk[2] = -this.zk[2];
            if( this.zk[5] < 0.0 )
                this.zk[5] = -this.zk[5];
        }  // end iterations
    }  // end compute_calibration()


    
    ////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////
    
    // resets the current approximation to the solution
    private void reset_zk()
    {
        for( int i = 0; i < this.zk.length; i++ ) {
            this.zk[i] = 0.0;
        }
        this.zk[0] = 1.0;
        this.zk[1] = 0.0;
        this.zk[2] = 1.0;
        this.zk[3] = 0.0;
        this.zk[4] = 0.0;
        this.zk[5] = 1.0;
        this.zk[6] = 0.0;
        this.zk[7] = 0.0;
        this.zk[8] = 0.0;
    }


    // resets the tensors
    private void reset_tensors()
    {
        this.W = 0.0;
        // first we reset the tensors
        for( int i = 0; i < 4; i++ ) {
            for( int j = 0; j < 4; j++ ) {
                for( int n = 0; n < this.maxN21; n++ )
                    this.Y2[n][i][j] = 0.0;
                for( int k = 0; k < 4; k++ ) {
                    for( int l = 0; l < 4; l++ ) {
                        for( int n = 0; n < this.maxN41; n++ )
                            this.X4[n][i][j][k][l] = 0.0;
                    }
                }
            }
        }
    }


    // updates the matrix A with the current approximation to the solution
    private void update_A()
    {
        for( int n = 0; n <= this.N; n++ ) {
            int n9 = 9 * n;
            this.A[n][0][0] = this.zk[n9];
            this.A[n][0][1] = 0.0;
            this.A[n][0][2] = 0.0;
            this.A[n][0][3] = this.zk[n9 + 6];
            this.A[n][1][0] = this.zk[n9 + 1];
            this.A[n][1][1] = this.zk[n9 + 2];
            this.A[n][1][2] = 0.0;
            this.A[n][1][3] = this.zk[n9 + 7];
            this.A[n][2][0] = this.zk[n9 + 3];
            this.A[n][2][1] = this.zk[n9 + 4];
            this.A[n][2][2] = this.zk[n9 + 5];
            this.A[n][2][3] = this.zk[n9 + 8];
        }
    }


    // gets a term of the matrix J^T*W*( y^2 - f )
    private double get_JTWdy( int g , int a , int b )
    {
        double sum = 0.0;
        for( int n2 = 0; n2 <= this.N; n2++ ) {
            int nY = n2 + g;
            for( int j2 = 0; j2 < 4; j2++ ) {
                sum += this.A[n2][a][j2] * this.Y2[nY][j2][b];
            }
            for( int n = 0; n <= this.N; n++ ) {
                for( int l = 0; l <= this.N; l++ ) {
                    int nX = nY + n + l;
                    for( int i = 0; i < 3; i++ ) {
                        for( int j1 = 0; j1 < 4; j1++ ) {
                            for( int k = 0; k < 4; k++ ) {
                                for( int j2 = 0; j2 < 4; j2++ ) {
                                    sum -= this.A[n2][a][j2] * this.A[n][i][j1] * this.A[l][i][k]
                                            * this.X4[nX][j2][b][j1][k];
                                }  // j2
                            }  // k
                        }  // j
                    }  // i
                }  // l
            }  // n
        }  // n2
        return 2.0 * sum;
    }


    // gets the matrix J^T*W*( y^2 - f )
    private double[] get_JTWdy()
    {
        // now we compute the matrix J^T*W*dy
        double[] JTWdy = new double[9 * ( this.N + 1 )];
        int iJ = 0;
        for( int g = 0; g <= this.N; g++ ) {
            // K part
            for( int a = 0; a < 3; a++ ) {
                for( int b = 0; b <= a; b++ ) {
                    JTWdy[iJ++] = this.get_JTWdy( g , a , b );
                }  // end b
            }  // end a
               // c part
            for( int a = 0; a < 3; a++ ) {
                JTWdy[iJ++] = this.get_JTWdy( g , a , 3 );
            }  // end a
        }  // end g
        return JTWdy;
    }  // get_JTWdy()


    // gets a term of the matrix J^T*W*J
    private double get_JTWJ( int g1 , int a1 , int b1 , int g2 , int a2 , int b2 )
    {
        double sum = 0.0;
        for( int n1 = 0; n1 <= this.N; n1++ ) {
            for( int n2 = 0; n2 <= this.N; n2++ ) {
                int nX = n1 + g1 + n2 + g2;
                for( int j1 = 0; j1 < 4; j1++ ) {
                    for( int j2 = 0; j2 < 4; j2++ ) {
                        sum += this.A[n1][a1][j1] * this.A[n2][a2][j2] * this.X4[nX][j1][b1][j2][b2];
                    }  // j2
                }  // j
            }  // n2
        }  // n
        return 4.0 * sum;
    }


    // gets the matrix J^T*W*J
    private double[][] get_JTWJ()
    {
        // now we compute the matrix J^T*W*J
        double[][] JTWJ = new double[9 * ( this.N + 1 )][9 * ( this.N + 1 )];
        int iJ1 = 0;
        for( int g = 0; g <= this.N; g++ ) {
            // K# part
            for( int a = 0; a < 3; a++ ) {
                for( int b = 0; b <= a; b++ ) {
                    int iJ2 = 0;
                    for( int g2 = 0; g2 <= this.N; g2++ ) {
                        // K part
                        for( int a2 = 0; a2 < 3; a2++ ) {
                            for( int b2 = 0; b2 <= a2; b2++ ) {
                                JTWJ[iJ1][iJ2] = this.get_JTWJ( g , a , b , g2 , a2 , b2 );
                                iJ2++;
                            }
                        }
                        // c part
                        for( int a2 = 0; a2 < 3; a2++ ) {
                            JTWJ[iJ1][iJ2] = this.get_JTWJ( g , a , b , g2 , a2 , 3 );
                            iJ2++;
                        }
                    }
                    iJ1++;
                }
            }
            // c# part
            for( int a = 0; a < 3; a++ ) {
                int iJ2 = 0;
                for( int g2 = 0; g2 <= this.N; g2++ ) {
                    // K part
                    for( int a2 = 0; a2 < 3; a2++ ) {
                        for( int b2 = 0; b2 <= a2; b2++ ) {
                            JTWJ[iJ1][iJ2] = this.get_JTWJ( g , a , 3 , g2 , a2 , b2 );
                            iJ2++;
                        }
                    }
                    // c part
                    for( int a2 = 0; a2 < 3; a2++ ) {
                        JTWJ[iJ1][iJ2] = this.get_JTWJ( g , a , 3 , g2 , a2 , 3 );
                        iJ2++;
                    }
                }
                iJ1++;
            }
        }
        return JTWJ;
    }  // end get_JTWJ()


    // Method: Cholesky
    // performs the Cholesky decomposition of a positive definite matrix ( S = L*L'
    // )
    // inputs:
    // S: NxN positive definite matrix to be decomposed (must be stored by columns)
    // outputs:
    // S: the lower triangular matrix L (6x6) is overwritten in S (is stored by
    // columns)
    private void Cholesky( double[][] S , int n )
    {
        // for each column
        for( int j = 0; j < n; j++ ) {
            double sum = 0.0;  // sum for the diagonal term
            // we first fill with 0.0 until diagonal
            for( int i = 0; i < j; i++ ) {
                S[i][j] = 0.0;
                // we can compute this sum at the same time
                sum += S[j][i] * S[j][i];
            }
            // now we compute the diagonal term
            S[j][j] = Math.sqrt( S[j][j] - sum );
            // finally we compute the terms below the diagonal
            for( int i = j + 1; i < n; i++ ) {
                // first the sum
                sum = 0.0;
                for( int k = 0; k < j; k++ ) {
                    sum += S[i][k] * S[j][k];
                }
                // after the non-diagonal term
                S[i][j] = ( S[i][j] - sum ) / S[j][j];
            }
        }// end j

        return;
    }


    // Method: solve
    // solves the system of linear equations K*S = M for K
    // inputs:
    // S: nxn positive definite matrix
    // M: 1xn matrix stored by rows
    // outputs:
    // M: K (1xn) is stored in the M memory space
    private void solve( double[][] S , double[] M , int n )
    {
        // we first compute the Cholesky decomposition for transform the system from K*S
        // = M into K*L*L' = M
        this.Cholesky( S , n );

        // first we solve (y*L' = M)
        for( int j = 0; j < n; j++ ) {
            double sum = M[j];
            for( int k = 0; k < j; k++ ) {
                sum -= M[k] * S[j][k];
            }
            M[j] = sum / S[j][j];
        }
        // now we solve (Ki*L = y)
        for( int j = n - 1; j > -1; j-- ) {
            double sum = M[j];
            for( int k = j + 1; k < n; k++ ) {
                sum -= M[k] * S[k][j];
            }
            M[j] = sum / S[j][j];
        }

        return;
    }

}

