package ru.m_polukhin.debtsapp.utils;

import net.objecthunter.exp4j.ExpressionBuilder;
import ru.m_polukhin.debtsapp.exceptions.ParseException;

public class Calculator {
    public static long evaluateExpression(String expression) throws ParseException {
        try {
            double result = new ExpressionBuilder(expression).build().evaluate();
            return Math.round(result);
        } catch (Exception e) {
            throw new ParseException(e.getMessage());
        }
    }
}
