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

* **Black Box Abstraction**: An abstraction in which the implementation details are not prerequisite for usage. One need only think about inputs and outputs.
* **White Box Abstraction**: An abstraction in which in which understanding the implementation details are required for usage.

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

[.code-highlight: 12]
[.code-highlight: 4-11]
[.code-highlight: 14-17]

```scala
import cats._
import cats.implicits._

object CommittableElement {
  implicit val functor = new Functor[CommittableElement] {
    override def map[A, B](fa: CommittableElement[A])(f: A => B): CommittableElement[B] = {
      CommittableElement(fa.committableOffset, f(fa.element))
    }
  }
}

case class CommittableElement[E](committableOffset: CommittableOffset, element: E)

object Main extends App {
  CommittableElement(offset, 6).map(_ + 1)
}
```

---

## Example Continued

```scala
def greet(str: String): Either[Throwable, String] = {
  if (str.contains("boom")) Left(new RuntimeException("boom"))
  else Right(s"$str world")
}

val nestedHello = CommittableElement(offset, Either.right[Throwable, String]("hello"))
val nestedBoom = CommittableElement(offset, Either.right[Throwable, String]("boom"))

EitherT(nestedHello).map(_.toUpperCase).subflatMap(greet)
// CommittableElement(offset, Right("HELLO world"))

EitherT(nestedBoom).subflatMap(greet)
// CommittableElement(offset, Left(RuntimeException("boom")))
```

---

## Functional Programming (FP)

*Pure functions* are a **black box abstraction**:
* Only need to understand the parameters and the return type
* If you want to be pedantic, you could point out that no function is a true black box
  * Time
  * Cache effects
  * Energy usage
  * Cosmic rays
* The point is that for **most** uses, you can think of pure functions as a black box; implementation does not matter
* By using libraries built around FP, we can:
  * Build components that all behave similarly
  * Reason about our components in isolation
  * Combine components together without boilerplate

---

## Category Theory

My definition: Formal study of abstractions

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

