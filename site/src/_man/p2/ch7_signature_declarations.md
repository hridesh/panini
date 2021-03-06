---
part: 2
chapter: 7
title: Signature Declarations
---

A signature is to a capsule as an interface is to a class. A signature is the
equivalent of an interface in object-oriented programming. It contains one or
more abstract procedure signatures. Each procedure signature has a return type,
a name, and zero or more formal parameters. This syntax is similar to interfaces
in Java, except that for simplicity we do not allow signature inheritance.
Capsules may implement multiple interfaces. Let us extract a signature from the
BankAccount capsule from above:

**Listing 7.1:** *A signature for bank accounts*

``` java
signature BankAccountSig{
  void withdraw(double money);
  void deposit(double money);
}
```
{: .code-with-line-numbers}

To have the `BankAccount` make use of this signature we write:

**Listing 7.2:** *Implementing a signature*

``` java
/* Since both methods declared in the signature were
   already present in our previous implementation of
   BankAccount, only this first line needs to be
   modified. */
capsule BankAccount implements BankAccountSig {
  double balance;
  => {
   balance = 100.0;
  }

  void deposit(double money) {
    balance += money;
  }

  void withdraw(double money) {
    if(balance - money &lt; 0) {
      throw new InvalidTransactionException();
    }
    balance -= money;
  }
}
```
{: .code-with-line-numbers}
