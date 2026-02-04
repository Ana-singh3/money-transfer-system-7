// src/main/java/com/moneytransfersystem/domain/util/Money.java
package com.moneytransfersystem.domain.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        this.amount = amount.setScale(4, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) { return new Money(amount); }
    public BigDecimal asBigDecimal() { return amount; }
    public Money add(Money other) { return new Money(this.amount.add(other.amount)); }
    public Money subtract(Money other) { return new Money(this.amount.subtract(other.amount)); }
    public boolean isLessThan(Money other) { return this.amount.compareTo(other.amount) < 0; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.equals(money.amount);
    }

    @Override
    public int hashCode() { return Objects.hash(amount); }

    @Override
    public String toString() { return amount.toPlainString(); }
}
