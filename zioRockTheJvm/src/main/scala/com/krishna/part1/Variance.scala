package com.krishna.part1

object Variance {

  // OOP - substitution
  class Animal
  class Dog(name: String) extends Animal

  // Variance question for List: if Dog <: Animal, then should List[Dog] <: List[Animal]?

  // 1. YES - COVARIANT
  val lassie = new Dog("Lassie")
  val hachi  = new Dog("Hachi")
  val laika  = new Dog("Laika")

  val anAnimal: Animal         = lassie
  val someAnimal: List[Animal] = List(lassie, hachi, laika)

  // class MyList[A]
  // val myAnimalList: MyList[Animal] = new MyList[Dog] // Wrong

  class MyList[+A] // MyList is COVARIANT in A
  val myAnimalList: MyList[Animal] = new MyList[Dog] // Right

  // 2. No - then the type is INVARIANT
  trait Semigroup[A] {
    def combine(x: A, y: A): A
  }

  // all generics in Java
  // val aJavaList: java.util.ArrayList[Animal] = new util.ArrayList[Dog]()

  // 3. HELL NO - CONTRAVARIANCE
  trait Vet[-A] {
    def heal(animal: A): Boolean
  }

  // Vet[Animal] is "better" than a Vet[Dog]: Vet can treat ANY animal, therefore my dog as well
  // Dog <: Animal, then Vet[Dog] >: Vet[Animal]
  val myVet: Vet[Dog] = new Vet[Animal] {
    override def heal(animal: Animal): Boolean = {
      println("Heal the animal...")
      true
    }
  }

  val healingLassie = myVet.heal(lassie)

  /*
    Rule of thumb:
    - if the type PRODUCES or RETRIEVES values of type A (e.g. lists), then the type should be COVARIANT
    - if the type CONSUMES or ACTS ON values of type A (e.g. a vet), then the type should be CONTRAVARIANT
    - otherwise, INVARIANT
   */
}
