package tryscalaz


import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ApplicativeSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._

  describe("applicatives") {

    it("list as applicatives should work") {
      // List as applicative
      List(10, 20, 30) <*> (List(1, 2, 3) map ((_: Int) * (_: Int)).curried) should equal(List(10, 20, 30, 20, 40, 60, 30, 60, 90))

      // filter (>50) $ (*) <$> [2,5,10] <*> [8,10,11] << from lyah
      (List(8, 10, 11) <*> (List(2, 5, 10) map ((_: Int) * (_: Int)).curried)).filter(_ > 50) should equal(List(55, 80, 100, 110))

      // (++) <$> ["ha","heh","hmm"] <*> ["?","!","."] << from lyah
      val c2 = ((_ : String) ++ (_: String)).curried
      List("?", "!", ".") <*> (List("ha", "heh", "hmm") map c2) should equal(List("ha?", "ha!", "ha.", "heh?", "heh!", "heh.", "hmm?", "hmm!", "hmm."))

      // List as functor : works the same way as Option as functor
      List(10, 20, 30, 40) map {x: Int => x * 2} should equal(List(20, 40, 60, 80))
    }

    it("option as applicatives should work") {
      // Option as functor which gets pimped by map defined in MA.scala
      // map takes a pure function & lifts it within the functor
      Some(10) map {x: Int => x * 2} should equal(Some(20))

      // use an implicit to make Option an applicative
      // note small lettered some() to take care of subtyping
      some(10) <*> (some(20) map ((_: Int) * (_: Int)).curried) should equal(Some(200))

      // simple applicative for option[]
      // Just (+3) <*> Just 9 << from lyah
      // quiz: why doesn't some((_: Int) + 3) <*> some(12) compile?
      // hint: follow the types
      some(9) <*> some((_: Int) + 3) should equal(Some(12))

      // pure (+3) <*> Just 10 << from lyah
      10.pure[Option] <*> some((_: Int) + 3) should equal(Some(13))

      // pure (+) <*> Just 3 <*> Just 5 << lyah
      // Note how pure lifts the function into Option applicative
      // scala> multc.pure[Option]
      // res6: Option[(Int) => (Int) => Int] = Some(<function1>)

      // scala> multc
      // res7: (Int) => (Int) => Int = <function1>
      val p2c = ((_: Int) * (_: Int)).curried
      some(5) <*> (some(3) <*> p2c.pure[Option]) should equal(Some(15))

      // none if any one is none
      some(9) <*> none should equal(none)

      // (++) <$> Just "johntra" <*> Just "volta" << lyah
      some("volta") <*> (some("johntra") map (((_: String) ++ (_: String)).curried)) should equal(Some("johntravolta"))
    }

    it("functions as applicatives should work") {
      // [(*0),(+100),(^2)] <*> [1,2,3] << lyah
      List(1, 2, 3) <*> List((_: Int) * 0, (_: Int) + 100, math.pow((_: Int), 2)) should equal(List(0, 0, 0, 101, 102, 103, 1.0, 4.0, 9.0))

      // applicatives on list and functions
      // [(+),(*)] <*> [1,2] <*> [3,4] << from lyah
      val a2 = ((_: Int) + (_: Int)).curried
      val p2 = ((_: Int) * (_: Int)).curried
      List(3, 4) <*> (List(1, 2) <*> List(a2, p2)) should equal(List(4, 5, 5, 6, 3, 4, 6, 8))

      // (*) <$> [2,5,10] <*> [8,10,11] << lyah
      List(8, 10, 11) <*> (List(2, 5, 10) map p2) should equal(List(16, 20, 22, 40, 50, 55, 80, 100, 110))
    }

    it("case classes and currying should work") {
      val movie1 = Map("title" -> "South Park", "user" -> "Terrence", "rating" -> "3")
      case class MovieReview(revTitle: String, revUser: String, revReview: String)
      movie1.get("title") <*> (movie1.get("user") <*> (movie1.get("rating") map MovieReview.curried)) should equal(Some(MovieReview("3", "Terrence", "South Park")))
    }

    it("lifts on applicatives should work") {
      // lift 2 for List
      List(10, 20, 30).<**>(List(1, 2, 3))(_ * _) should equal(List(10, 20, 30, 20, 40, 60, 30, 60, 90)) 

      // lift 2 for Option
      some(5).<**>(some(3))(_ * _) should equal(Some(15))

      // liftA2 (:) (Just 3) (Just [4]) << lyah
      some(3).<**>(some(List(4)))(_ :: _) should equal(Some(List(3, 4)))

      // same using map
      // (:) <$> Just 3 <*> Just [4]
      some(List(4)) <*> (some(3) map ((_: Int) :: (_: List[Int])).curried) should equal(Some(List(3, 4)))

      // lift Option into pair
      some(5).<|*|>(some(3)) should equal(Some((5, 3)))

      // Lift-2 the List Applicative functor to a pair
      (List(1, 2, 3) <|*|> List(40, 50, 60)) should equal(List((1, 40), (1, 50), (1, 60), (2, 40), (2, 50), (2, 60), (3, 40), (3, 50), (3, 60)))

      // lift 3 into tuple
      some(5).<|**|>(some(3), some(12)) should equal(Some((5, 3, 12)))
    }
  }
}
