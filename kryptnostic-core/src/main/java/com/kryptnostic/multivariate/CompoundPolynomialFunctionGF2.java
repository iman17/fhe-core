package com.kryptnostic.multivariate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Basic implementation of CompoundPolynomialFunction over GF2, consisting of a linked list of functions.
 * The input to the function is fed to first function in the list with the overall output of a compound 
 * function being determined by traversing the linked list from the beginning and evaluating each function
 * on the output of the previous function. The output of the last function is the output of evaluating the 
 * function. 
 * @author Matthew Tamayo-Rios
 */
public class CompoundPolynomialFunctionGF2 implements CompoundPolynomialFunction {
    
    private final LinkedList<SimplePolynomialFunction> functions;
    
    public CompoundPolynomialFunctionGF2() {
        this( ImmutableList.<SimplePolynomialFunction>of() );
    }
    
    public CompoundPolynomialFunctionGF2( List<SimplePolynomialFunction> functions ) {
        this.functions = Lists.newLinkedList( functions );
    }
    
    public CompoundPolynomialFunction compose( CompoundPolynomialFunction inner ) {
        validateForCompose( inner );
        CompoundPolynomialFunctionGF2 newCPF = new CompoundPolynomialFunctionGF2();
        
        newCPF.functions.addAll( inner.getFunctions() );
        newCPF.functions.addAll( functions );
        return newCPF;
    }
    
    @Override
    public CompoundPolynomialFunction compose( SimplePolynomialFunction inner ) {
        validateForCompose( inner );
        CompoundPolynomialFunctionGF2 cpf = copy();
        cpf.functions.addFirst( inner );
        return cpf;
    }
    
    @Override
    public CompoundPolynomialFunction prefix( SimplePolynomialFunction inner ) {
        validateForCompose( inner );
        functions.addFirst( inner );
        return this;
    }
    @Override
    public CompoundPolynomialFunction suffix( SimplePolynomialFunction inner) {
       Preconditions.checkArgument( 
               getOutputLength() == inner.getInputLength() , 
               "Function being appeneded must have the same length." ); 
       functions.addLast( inner );
       return this;
    }
    @Override
    public CompoundPolynomialFunctionGF2 copy() {
        CompoundPolynomialFunctionGF2 cpf = new CompoundPolynomialFunctionGF2();
        cpf.functions.addAll( this.functions );
        return cpf;
    }

    @Override
    public BitVector apply(BitVector input) {
        BitVector result = input;
        
        for( PolynomialFunction f : functions ) {
            result = f.apply( result );
        }
        
        return result;
    }
    
    @Override
    public BitVector apply(BitVector lhs, BitVector rhs) {
        Preconditions.checkArgument( 
                ( lhs.size() + rhs.size() ) == getInputLength() , 
                "Vectors provided for evaluation must have the same total length as the function expects as input."); 
        return apply( FunctionUtils.concatenate( lhs , rhs ) );
    }
    
    @Override
    public int getInputLength() {
        if( functions.isEmpty() ) {
            return 0;
        }
        return functions.getFirst().getInputLength();
    }

    @Override
    public int getOutputLength() {
        if( functions.isEmpty() ) {
            return 0;
        }
        return functions.getLast().getOutputLength();
    }

    public void validateForCompose( PolynomialFunction inner ) {
        if( getInputLength() != 0 ) { 
            Preconditions.checkArgument( 
                    getInputLength() == inner.getOutputLength() ,
                    "Input length of outer function must match output length of inner function it is being composed with"
                    );
        }
    }

    @Override
    public List<SimplePolynomialFunction> getFunctions() {
        return Collections.unmodifiableList( functions );
    }
    
    @Override
    public int count() {
        return functions.size();
    }

    @Override
    public void composeHeadDirectly(SimplePolynomialFunction inner) {
        Preconditions.checkArgument( functions.getFirst() instanceof SimplePolynomialFunction , "Cannot compose function that isn't of type SimplePolynomialFunction." );
        SimplePolynomialFunction outer = (SimplePolynomialFunction) functions.getFirst();
        functions.set( 0 , outer.compose( inner ) );
    }
}
