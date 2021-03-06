package com.kryptnostic.linear;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class MatrixTests {
    private static final Logger logger = LoggerFactory.getLogger( MatrixTests.class );
    
    /**
     * This test may randomly fail if 100 singular matrices are chosen in a row.
     * @throws SingularMatrixException
     */
    @Test
    public void inverseAndMultiplyTest() throws SingularMatrixException {
        int count = 0 ;
        boolean success = false;
        while( count < 100 ) {
            EnhancedBitMatrix m = EnhancedBitMatrix.randomSqrMatrix( 10 );
            EnhancedBitMatrix mcpy = new EnhancedBitMatrix( m );
            EnhancedBitMatrix minv;
            try {
                minv = m.inverse();
                Assert.assertEquals( m , mcpy);
                EnhancedBitMatrix identity = minv.multiply( m );
                Assert.assertEquals( identity , EnhancedBitMatrix.identity( 10 ) );
                success = true;
                break;
            } catch( SingularMatrixException e ) {
                logger.error( "Singlur matrices cannot be inverted." , e );
                ++count;
            }
            logger.trace("M^-1 = {}", m );
        }
        Assert.assertEquals( success , true );
    }
    
    @Test
    public void vectorMultipltyTest() {
        /*
         * Build out predetermined matrix
         * 
         * 1 0 1 1
         * 0 1 0 1
         */ 

        BitVector row1 = new BitVector( 4 );
        BitVector row2 = new BitVector( 4 );
        
        row1.set( 0 );row1.set( 2 );row1.set( 3 );
        row2.set( 1 );row2.set( 3 );
        
        /*         
         * Build out predetermined vectors
         * 
         * v1 -> { 0 , 0 }
         * 1
         * 1
         * 0
         * 1
         * 
         * v2 -> { 0 , 0 }
         * 1
         * 0
         * 1
         * 0
         * 
         * v3 -> { 1 , 1 }
         * 0
         * 1
         * 1
         * 0
         */
        
        BitVector v1 = new BitVector( 4 );
        v1.set( 0 );v1.set( 1 );v1.set( 3 );
        
        BitVector v2 = new BitVector( 4 );
        v2.set( 0 );v2.set( 2 );
        
        BitVector v3 = new BitVector( 4 );
        v3.set( 1 );v3.set( 2 );
        
        EnhancedBitMatrix m = new EnhancedBitMatrix( Lists.newArrayList( row1, row2 ) );
        
        logger.trace( "v1 = {} , v2 = {} , v3 = {}" , v1 , v2 , v3 );
        
        BitVector r1 = m.multiply( v1 );
        Assert.assertEquals( r1.size() , 2 );
        BitVector r2 = m.multiply( v2 );
        Assert.assertEquals( r2.size() , 2 );
        BitVector r3 = m.multiply( v3 );
        Assert.assertEquals( r3.size() , 2 );
        logger.trace( "r1 = {} , r2 = {} , r3 = {}" , r1 , r2 , r3 );
        
        Assert.assertEquals( r1.get( 0 ) , false );
        Assert.assertEquals( r1.get( 1 ) , false );
        
        Assert.assertEquals( r2.get( 0 ) , false );
        Assert.assertEquals( r2.get( 1 ) , false );
        
        Assert.assertEquals( r3.get( 0 ) , true );
        Assert.assertEquals( r3.get( 1 ) , true );
    }
    
    @Test 
    public void transposeTest() {
        EnhancedBitMatrix m = EnhancedBitMatrix.randomMatrix(63 , 65);
        EnhancedBitMatrix mt = m.transpose();
        
        for( int row = 0 ; row < m.rows() ; ++row ) {
            for( int col = 0 ; col < m.cols() ; ++col ) {
                Assert.assertEquals( m.get( row , col ) , mt.get( col , row ) ); 
            }
        }
        
        m = EnhancedBitMatrix.randomMatrix(65 , 63);
        mt = m.transpose();
        
        for( int row = 0 ; row < m.rows() ; ++row ) {
            for( int col = 0 ; col < m.cols() ; ++col ) {
                Assert.assertEquals( m.get( row , col ) , mt.get( col , row ) ); 
            }
        }
    }
    
    @Test
    public void zeroTest() {
        EnhancedBitMatrix m = new EnhancedBitMatrix( 4 , 6 );
        Assert.assertEquals( true , m.isZero() );
        m.set( 1  ,  5 );
        Assert.assertEquals( false , m.isZero() );
    }
    
    @Test 
    public void nullifyingTest() throws SingularMatrixException {
        EnhancedBitMatrix m = EnhancedBitMatrix.randomMatrix( 210 , 105  );
        while( m.nullspace().rows() < 65 ) {
            m = EnhancedBitMatrix.randomMatrix( 210 , 105  );
        }
        EnhancedBitMatrix nsBasis = m.getLeftNullifyingMatrix();
        Assert.assertEquals( nsBasis.rows() , m.cols() );
        Assert.assertEquals( nsBasis.cols() , m.rows() );
        logger.trace( "Nullifying matrix ({},{}): {}" , nsBasis.rows() , nsBasis.cols() , nsBasis );

        EnhancedBitMatrix result = nsBasis.multiply( m );
        Assert.assertEquals( true , result.isZero() );
        logger.trace(  "Nullified: {}" , result );
    }
    
    @Test 
    public void nullspaceTest() {
        EnhancedBitMatrix m = EnhancedBitMatrix.randomMatrix( 65 , 210 );
        EnhancedBitMatrix nsBasis = m.getNullspaceBasis();
        while( nsBasis.rows() == 0) {
            m = EnhancedBitMatrix.randomMatrix( 65 , 210 );
            nsBasis = m.getNullspaceBasis();
        }
        logger.trace( "Nullspace basis ({},{}): {}" , nsBasis.rows() , nsBasis.cols() , nsBasis );
        EnhancedBitMatrix result = m.multiply( nsBasis );
        logger.trace(  "Nullified: {}" , result );
        Assert.assertEquals( true , result.isZero() );
    }
    
    @Test
    public void generalizedInverseTest() throws SingularMatrixException {
        EnhancedBitMatrix m = EnhancedBitMatrix.randomMatrix( 65 , 257 );
        Assert.assertEquals( EnhancedBitMatrix.identity( 65 ) , m.multiply( m.rightGeneralizedInverse() ) );
        
        m = EnhancedBitMatrix.randomMatrix( 257 , 65 );
        Assert.assertEquals( EnhancedBitMatrix.identity( 65 ) , m.leftGeneralizedInverse().multiply( m ) );
    }
    
    @Test
    public void testPolynomialFunctionMultiply() {
        //Generate test function 
        SimplePolynomialFunction f = SimplePolynomialFunctions.randomFunction(256, 256);
        //Generate test matrices
        EnhancedBitMatrix m1 = EnhancedBitMatrix.randomMatrix( 256 , 256 );
        EnhancedBitMatrix m2 = EnhancedBitMatrix.randomMatrix( 512 , 256 );
        EnhancedBitMatrix m3 = EnhancedBitMatrix.randomMatrix( 128 , 256 );
        //Generate test vectors
        BitVector v1 = BitVectors.randomVector( 256 );
        BitVector v2 = BitVectors.randomVector( 256 );
        BitVector v3 = BitVectors.randomVector( 256 );
        //Multiply vectorial polynomial function by matrix
        SimplePolynomialFunction r1 = m1.multiply( f );
        SimplePolynomialFunction r2 = m2.multiply( f );
        SimplePolynomialFunction r3 = m3.multiply( f );
        //Compute expected values
        BitVector ev1 = m1.multiply( f.apply( v1 ) );
        BitVector ev2 = m2.multiply( f.apply( v2 ) );
        BitVector ev3 = m3.multiply( f.apply( v3 ) );
        //Compute actual values
        BitVector av1 = r1.apply( v1 );
        BitVector av2 = r2.apply( v2 );
        BitVector av3 = r3.apply( v3 );
        
        logger.trace( "Expected output for v1: {}" , ev1 );
        logger.trace( "Actual output for v1: {}" , av1 );
        Assert.assertEquals( ev1 , av1 );
        
        logger.trace( "Expected output for v2: {}" , ev2 );
        logger.trace( "Actual output for v1: {}" , av2 );
        Assert.assertEquals( ev2 , av2 );
        
        logger.trace( "Expected output for v3: {}" , ev3 );
        logger.trace( "Actual output for v1: {}" , av3 );
        Assert.assertEquals( ev3 , av3 );
        
    }
 
    @Test
    public void testSquareMatrixFromBitVector() {
        BitVector expected = BitVectors.randomVector( 256 );
        EnhancedBitMatrix m = EnhancedBitMatrix.squareMatrixfromBitVector( expected );
        BitVector actual = BitVectors.fromSquareMatrix( m );
        Assert.assertEquals( expected , actual );
    }
    
    @Test
    public void testLeftInverse() {
        EnhancedBitMatrix m = null;
        EnhancedBitMatrix mInv = null;
        for( int i = 0 ; i < 100; ++i ) {
            int notDone = 1000;

            while( notDone > 0 ) {
                m = EnhancedBitMatrix.randomMatrix( 256 , 128 );
                
                try {
                    mInv = m.leftInverse();
                    Assert.assertNotNull( mInv );
                    EnhancedBitMatrix result = mInv.multiply( m );
                    Assert.assertTrue( result.isIdentity() );
                    notDone = 0;
                } catch (SingularMatrixException e) {
                    m = null;
                    mInv = null;
                    logger.trace( "Encountered singluar matrix... trying again." );
                    --notDone;
                }
            }
        }
    }
    
    @Test
    public void testRightInverse() {
        EnhancedBitMatrix m = null;
        EnhancedBitMatrix mInv = null;
        for( int i = 0 ; i < 100; ++i ) {
            int notDone = 1000;

            while( notDone > 0 ) {
                m = EnhancedBitMatrix.randomMatrix( 128 , 256 );
                
                try {
                    mInv = m.rightInverse();
                    Assert.assertNotNull( mInv );
                    EnhancedBitMatrix result = m.multiply( mInv );
                    Assert.assertTrue( result.isIdentity() );
                    notDone = 0;
                } catch (SingularMatrixException e) {
                    m = null;
                    mInv = null;
                    logger.trace( "Encountered singluar matrix... trying again." );
                    --notDone;
                }
            }
        }
    }
    
}
