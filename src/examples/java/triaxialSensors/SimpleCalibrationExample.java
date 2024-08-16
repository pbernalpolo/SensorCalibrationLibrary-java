package triaxialSensors;


import java.io.File;
import java.util.Random;

import sensorCalibrationLibrary.triaxialSensors.Bernal2019TriaxialCalibration;
import sensorCalibrationLibrary.triaxialSensors.Bernal2019TriaxialCalibrator;



public class SimpleCalibrationExample
{
    
    public static void main( String[] args )
    {
        // Generate calibration data.
        
        int samples = 1000;
        
        Random randomNumberGenerator = new Random( 42 );
        
        double[][] x = new double[samples][3];
        for( int n=0; n<samples; n++ ) {
            double[] xyz = new double[3];
            xyz[0] = randomNumberGenerator.nextGaussian();
            xyz[1] = randomNumberGenerator.nextGaussian();
            xyz[2] = randomNumberGenerator.nextGaussian();
            double xyzNorm = Math.sqrt( xyz[0] * xyz[0]  +  xyz[1] * xyz[1]  +  xyz[2] * xyz[2] );
            x[n][0] = xyz[0] / xyzNorm;
            x[n][1] = xyz[1] / xyzNorm;
            x[n][2] = xyz[2] / xyzNorm;
        }
        
        
        // Calibrate.
        
        Bernal2019TriaxialCalibrator calibrator = new Bernal2019TriaxialCalibrator();
        calibrator.setPolynomialOrder( 0 );
        for( int n=0; n<samples; n++ ) {
            calibrator.addCalibrationData( 1.0 , x[n][0] , x[n][1] , x[n][2] , 0.0 , 1.0 );
        }
        calibrator.calibrate();
        
        Bernal2019TriaxialCalibration calibration = calibrator.getCalibration();
        new File( "./data/output/" ).mkdirs();
        calibration.save( "./data/output/calibration.cal" );
        
        
        // Obtain calibrated data.
        
        double[][] xCalibrated = new double[samples][3];
        for( int n=0; n<samples; n++ ) {
            xCalibrated[n] = calibration.correct( x[n] );
        }
        
    }

    
}
