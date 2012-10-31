package life.v41d;

/**************************************************************
 LifeQueue.java

 A very generic queue class.
**************************************************************/

class LifeQueueItem
{
   Object obj;
   LifeQueueItem next;
}

class LifeQueue
{
   private LifeQueueItem firstIn;
   private LifeQueueItem lastIn;
   private int length;

   LifeQueue()
   {
      firstIn = lastIn = null;
      length = 0;
   }

   public void push(Object obj)
   {
      LifeQueueItem item = new LifeQueueItem();

      item.obj = obj;
      item.next = null;

      if (lastIn != null) lastIn.next = item;
      lastIn = item;
      if (firstIn == null) firstIn = item;

      length++;
   }

   public Object pull()     // first in, first out
   {
      Object obj = null;

      if (firstIn != null)
      {
         obj = firstIn.obj;
         firstIn = firstIn.next;
         if (firstIn == null) lastIn = null;
         length--;
      }

      return obj;
   }

   public int length()
   {
      return length;
   }
}
