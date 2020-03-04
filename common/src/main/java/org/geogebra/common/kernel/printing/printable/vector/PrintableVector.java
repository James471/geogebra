package org.geogebra.common.kernel.printing.printable.vector;

import org.geogebra.common.kernel.arithmetic.ExpressionValue;

public interface PrintableVector {

    ExpressionValue getX();

    ExpressionValue getY();

    boolean isCASVector();

    int getCoordinationSystem();
}
