package com.kryptnostic.multivariate.polynomial;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kryptnostic.multivariate.MultivariateUtils;
import com.kryptnostic.multivariate.PolynomialLabeling;
import com.kryptnostic.multivariate.gf2.Monomial;

/**
 * Base class for storing and transmission of polynomial representations.
 * 
 * @author Matthew Tamayo-Rios
 */
public class PolynomialFunctionRepresentationGF2 implements Serializable {
    private static final long     serialVersionUID       = 6408384700566922194L;
    protected static final String INPUT_LENGTH_PROPERTY  = "input-length";
    protected static final String OUTPUT_LENGTH_PROPERTY = "output-length";
    protected static final String MONOMIALS_PROPERTY     = "monomials";
    protected static final String CONTRIBUTIONS_PROPERTY = "contributions";

    protected final int           inputLength;
    protected final int           outputLength;
    protected final Monomial[]    monomials;
    protected final BitVector[]   contributions;

    @JsonCreator
    public PolynomialFunctionRepresentationGF2(
            @JsonProperty( INPUT_LENGTH_PROPERTY ) int inputLength,
            @JsonProperty( OUTPUT_LENGTH_PROPERTY ) int outputLength,
            @JsonProperty( MONOMIALS_PROPERTY ) Monomial[] monomials,
            @JsonProperty( CONTRIBUTIONS_PROPERTY ) BitVector[] contributions ) {
        this.inputLength = inputLength;
        this.outputLength = outputLength;
        this.monomials = monomials;
        this.contributions = contributions;
    }

    public PolynomialFunctionRepresentationGF2() {
        inputLength = 0;
        outputLength = 0;
        monomials = null;
        contributions = null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( contributions );
        result = prime * result + inputLength;
        result = prime * result + Arrays.hashCode( monomials );
        result = prime * result + outputLength;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }

        if ( obj == null ) {
            return false;
        }
        if ( !( obj instanceof PolynomialFunctionRepresentationGF2 ) ) {
            return false;
        }

        PolynomialFunctionRepresentationGF2 other = (PolynomialFunctionRepresentationGF2) obj;

        if ( outputLength != other.outputLength ) {
            return false;
        }
        if ( inputLength != other.inputLength ) {
            return false;
        }
        /*
         * Generate a map view of monomial contributions. If one map has no entries that are not in the other map and
         * both maps have the same number of monomials, then the functions are equal.
         */
        Map<Monomial, BitVector> thisMap = MultivariateUtils.mapViewFromMonomialsAndContributions(
                monomials,
                contributions );
        Map<Monomial, BitVector> objMap = MultivariateUtils.mapViewFromMonomialsAndContributions(
                other.monomials,
                other.contributions );

        for ( Entry<Monomial, BitVector> entry : thisMap.entrySet() ) {
            BitVector thisContribution = entry.getValue();
            BitVector otherContribution = objMap.get( entry.getKey() );
            if ( otherContribution == null && thisContribution != null ) {
                return false;
            }
            if ( !otherContribution.equals( thisContribution ) ) {
                return false;
            }
        }

        if ( monomials.length == other.monomials.length ) {
            return true;
        }

        return false;
    }

    public static class Builder {
        protected Map<Monomial, BitVector> monomials = Maps.newHashMap();
        protected final int                inputLength;
        protected final int                outputLength;

        public Builder( int inputLength, int outputLength ) {
            this.inputLength = inputLength;
            this.outputLength = outputLength;
        }

        public void addMonomial( Monomial monomial ) {
            if ( !monomials.containsKey( monomial ) ) {
                monomials.put( monomial, new BitVector( outputLength ) );
            } else {
                throw new InvalidParameterException( "Monomial " + monomial.toString() + " already exists." );
            }
        }

        public void addMonomialContribution( Monomial monomial, int outputBit ) {
            BitVector term = monomials.get( monomial );
            if ( term == null ) {
                term = new BitVector( outputLength );
                monomials.put( monomial, term );
            }
            term.set( outputBit );
        }

        public void setMonomialContribution( Monomial monomial, BitVector contribution ) {
            monomials.put( monomial, contribution );
        }

        public PolynomialFunctionRepresentationGF2 build() {
            Pair<Monomial[], BitVector[]> monomialsAndContributions = getMonomialsAndContributions();
            return new PolynomialFunctionRepresentationGF2(
                    inputLength,
                    outputLength,
                    monomialsAndContributions.getLeft(),
                    monomialsAndContributions.getRight() );
        }

        protected Pair<Monomial[], BitVector[]> getMonomialsAndContributions() {
            Monomial[] newMonomials = new Monomial[ monomials.size() ];
            BitVector[] newContributions = new BitVector[ newMonomials.length ];
            int index = 0;
            for ( Entry<Monomial, BitVector> entry : monomials.entrySet() ) {
                newMonomials[ index ] = entry.getKey();
                newContributions[ index ] = entry.getValue();
                ++index;
            }
            return Pair.<Monomial[], BitVector[]> of( newMonomials, newContributions );
        }

        protected PolynomialFunctionRepresentationGF2 make(
                int inputLength,
                int outputLength,
                Monomial[] monomials,
                BitVector[] contributions ) {
            return new PolynomialFunctionRepresentationGF2( inputLength, outputLength, monomials, contributions );
        }
    }

    @JsonProperty( INPUT_LENGTH_PROPERTY )
    public int getInputLength() {
        return inputLength;
    }

    @JsonProperty( OUTPUT_LENGTH_PROPERTY )
    public int getOutputLength() {
        return outputLength;
    }

    @JsonProperty( MONOMIALS_PROPERTY )
    public Monomial[] getMonomials() {
        return monomials;
    }

    @JsonProperty( CONTRIBUTIONS_PROPERTY )
    public BitVector[] getContributions() {
        return contributions;
    }

    @Override
    public String toString() {
        StringBuilder rep = new StringBuilder();
        for ( int row = 0; row < outputLength; ++row ) {
            boolean first = true;
            for ( int i = 0; i < monomials.length; ++i ) {
                if ( contributions[ i ].get( row ) ) {
                    if ( !first ) {
                        rep.append( " + " );
                    } else {
                        first = false;
                    }
                    rep.append( monomials[ i ].toStringMonomial() );
                }
            }
            rep.append( "\n" );
        }

        return rep.toString();
    }

    public String toLatexString() {
        return toLatexString( "\\mathbf x" );
    }

    @SuppressWarnings( "unchecked" )
    public String toLatexString( String var ) {
        Pair<String, Integer> baseLabel = Pair.of( var, monomials[0].size() );
        return toLatexString( "f", new PolynomialLabeling( baseLabel ) );
    }

    public String toLatexString( String functionName, PolynomialLabeling labels ) {
        // Build the header.
        StringBuilder rep = new StringBuilder( "\\begin{equation}\n" ).append( functionName ).append( "(" )
                .append( labels.getVarList() ).append( ") = \\left[ \\begin{array}{c}\n" );

        int skipIndex = outputLength - 1;
        for ( int row = 0; row < outputLength; ++row ) {
            List<String> monomialLabels = Lists.newArrayList();

            // For each output row write down all the monomials
            for ( int i = 0; i < monomials.length; ++i ) {
                if ( contributions[ i ].get( row ) ) {
                    monomialLabels.add( monomials[ i ].toLatexStringMonomial( labels ) );
                }
            }
            final int last = monomialLabels.size() - 1;

            for ( int i = 0; i < monomialLabels.size(); ++i ) {
                rep.append( monomialLabels.get( i ) );
                if ( i != last ) {
                    rep.append( " + " );
                }
            }
            if ( row == skipIndex ) {
                rep.append( "\n" );
            } else {
                rep.append( " \\\\\n" );
            }
        }
        rep.append( "\\end{array} \\right]\n\\end{equation} " );

        return rep.toString().trim();
    }

    public static PolynomialFunctionRepresentationGF2 randomFunction( int inputLen, int outputLen ) {
        PolynomialFunctionRepresentationGF2.Builder builder = PolynomialFunctionRepresentationGF2.builder(
                inputLen,
                outputLen );
        for ( int i = 0; i < 16; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( outputLen );
            builder.setMonomialContribution( Monomial.randomMonomial( inputLen, 3 ), contribution );
        }

        return builder.build();
    }

    public static Builder builder( int inputLength, int outputLength ) {
        return new Builder( inputLength, outputLength );
    }

}