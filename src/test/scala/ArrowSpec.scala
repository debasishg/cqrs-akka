package tryscalaz

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ArrowSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._

  /**
   * FizzBuzz using Arrows in Scalaz
   * Original Haskell version at http://www.storytotell.org/blog/2007/04/08/haskell-arrows.html
   * History of FizzBuzz : http://www.codinghorror.com/blog/2007/02/why-cant-programmers-program.html
   */
  def divides(divisor: Int) = { dividend: Int => dividend % divisor == 0 }

  def genTest[A, B](pred: A => Boolean, result: B) =
    { in: A => if (pred(in)) Right(result) else Left(in) }

  def three: Int => Either[Int, String] = genTest(divides(3), "Fizz")
  def five: Int => Either[Int, String] = genTest(divides(5), "Buzz")

  def combine(f: Either[Int, String], s: Either[Int, String]): String = (f, s) match {
    case (Left(x), Left(_)) => x.toString
    case (Right(x), Right(y)) => x ++ y
    case (Right(x), _) => x
    case (_, Right(y)) => y
  }

  def fizzbuzz = (three &&& five) >>> (a => combine(a._1, a._2))

  describe("arrow composition") {

    it("fizzbuzz should work") {
      (1 until 101).map(fizzbuzz(_)).filter(_ == "FizzBuzz").size should equal(6)
      (1 until 101).map(fizzbuzz(_)).filter(_ == "Fizz").size should equal(27)
      (1 until 101).map(fizzbuzz(_)).filter(_ == "Buzz").size should equal(14)
    }
  }
}
