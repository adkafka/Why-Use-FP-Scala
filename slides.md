slidenumbers: true
autoscale: true
theme: work

# [fit] Functional Programming in Scala 
# and Why We Should Care

---

## Our Goal

**Ship quality code**

This means code that:

* Is easy to test
* Is easy to re-use
* Provides a useful abstraction
* Handles failure by design
* Scales well, is performant, and robust (Akka solves this for us)

---

## Abstraction

My definition: A re-usable dimensionality reduction in a problem space

Abstractions are the holy grail in programming; they allow for thinking about a problem in isolation, without worrying about other components.

One emergent pattern has been the use of *polymorphism*: re-using a single interface on many concrete types, or using a single abstract type to represent many concrete types that follow some contract

---

## Polymorphism

Comes in three main forms:

1. **Parametric Polymorphism**: Generics (`List[A]`).
2. **Subtype Polymorphism**: Class inheritance (`class X extends Y`).
3. **Ad-Hoc Polymorphism**: Similar to Subtype Polymorphism, but more generalizable. In Scala, this comes from the use of implicit conversions or parameters.

[.footer:  http://eed3si9n.com/herding-cats/polymorphism.html]

---

## Ad-hoc Polymorphism

* Separates functions for different types of `A`
* Works without access to source code (`Int`, `Flow`, etc)
* Can be enabled and disabled in different scopes

[.footer:  http://eed3si9n.com/herding-cats/polymorphism.html]

---

## Ad-hoc Polymorphism Example

TODO: Give an example of a typeclass

---

## Functional Programming (FP)

* *Pure functions* are the perfect abstraction, so we should use them when possible:
  * Only need to understand the parameters and the return type
* By using libraries built around FP, we can:
  * Build components that all behave similarly
  * Reason about our components in isolation
  * Combine components together without boilerplate

---

## Category Theory

Branched from math

TODO: Breif definition, and explain how it intersects w/ FP

---

## Type Classes (Higher-Kinded types, Higher-Order types)

**Proper**
```
Int    String    Car    HttpRequst
```
**First-Order**
```
List[A]    Option[A]    Either[L, R]    Flow[A, B, M]
```
**Higher-Order**
```
Functor[F[_]]    Monoid[F[_]]    Monad[F[_]]
```

> Concretely, a higher-order abstraction abstracts over something that abstracts over something
- Aadrian Moors

[.footer: https://stackoverflow.com/questions/6246719/what-is-a-higher-kinded-type-in-scala/6427289#6427289]

---

## Type Class Examples (Cats)

* Functor

* Monoid

* Monad

---

## Cats

![right](images/cats2.png)

* Built by [TypeLevel](https://typelevel.org/)
* Top notch documentation with a helpful community
* Modular library
* **Many** libraries built around it

---

## Kafka Example

---

## API Example

