package ru.m_polukhin.debtsapp.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.m_polukhin.debtsapp.exceptions.ParseException;

public class CalculatorTest {
    @Test
    public void testEvaluateExpression() throws ParseException {
        Assertions.assertEquals(5, Calculator.evaluateExpression("(5 + 5) / 2"));
        Assertions.assertEquals(12, Calculator.evaluateExpression("2 * (3 + 3)"));
        Assertions.assertEquals(7, Calculator.evaluateExpression("20 / (1 * 3)"));
        Assertions.assertEquals(52, Calculator.evaluateExpression("(2 + 2) / (2 - 1) * (2*8-(1+2))"));
    }
}