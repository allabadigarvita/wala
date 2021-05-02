public class Dataflow {

  static int f;

  static int g;

  public static void testrd()
  {
    f = 4;
    g = 3;
    if (f == 5)
    {
      g = 2;
    }
    else
    {
      g = 7;
    }
  }

}
