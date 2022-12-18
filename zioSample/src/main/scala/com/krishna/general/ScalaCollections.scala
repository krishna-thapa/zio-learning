package com.krishna.general

object ScalaCollections extends App {

  /**
   * <b>Learn about LazyList</b>
   * A LazyList is like a list except that its elements are computed lazily.
   * Because of this, a lazy list can be infinitely long.
   * Only those elements requested are computed.
   * Otherwise, lazy lists have the same performance characteristics as lists.
   * */

  val lazyList = 1 #:: 2 #:: 3 #:: LazyList.empty
  println(s"Lazy list: $lazyList") //not computed

  val lazyList2 = LazyList(1, 2, 3, 4, 5)
  println(s"Lazy list part 2: $lazyList2") //not computed

  println(s"Lazy list head: ${lazyList2.head}") // Will compute
  println(s"Lazy list tail: ${lazyList2.tail}") //not computed
  println(s"Lazy list head of tail: ${lazyList2.tail.head}") // Will compute

  def fibFrom(a: Int, b: Int): LazyList[Int] = a #:: fibFrom(b, a + b)

  val fibs = fibFrom(1, 1).take(7)
  val fabsToList = fibs.toList
  println(s"Fib using lazy list: $fabsToList")

  // Read more: https://docs.scala-lang.org/overviews/collections-2.13/concrete-immutable-collection-classes.html
}
