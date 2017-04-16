import java.io.BufferedReader
import java.io.FileReader
import java.io.File
fun termIndexer() { 
  val br = BufferedReader(FileReader("./dataset/rakuten_reviews_wakati.txt"))
  val term_index = mutableMapOf<String, Int>()
  while(true) { 
    val line = br.readLine()
    if( line == null ) break
    val ents = line.split(" __ SEP __ ")
    if( ents.size < 2 ) continue
    val terms = ents[1]
    terms.split(" ").map { x -> 
      if( term_index.get(x) == null ){ 
        term_index[x] = term_index.size + 1
        println("${x} ${term_index[x]}")
      }
    }
  }
}

fun svmIndexer() {
  val term_index  = File("./dataset/term_index.txt").readText()
  val term_id     = term_index.split("\n").filter { x ->
    x != ""
  }.map { x -> 
    val (term, id) = x.split(" ")
    Pair(term, id)
  }.toMap()
  val br = BufferedReader(FileReader("./dataset/rakuten_reviews_wakati.txt"))
  while(true) { 
    val line = br.readLine()
    if( line == null ) break
    val ents = line.split(" __ SEP __ ")
    if( ents.size < 2 ) continue
    var stars = 0.0
    try { 
      stars = ents[0].replace(" ", "").toDouble()
    } catch ( e : java.lang.NumberFormatException ) { 
      continue 
    }
    val terms = ents[1]
    val term_freq = mutableMapOf<String, Double>()
    terms.split(" ").map { x -> 
      if ( term_freq[x] == null ) term_freq[x] = 0.0
      term_freq[x]  =  term_freq[x]!! + 1.0
    }
    val id_weight = term_freq.keys.map { k -> 
      Pair(term_id[k]!!, Math.log(term_freq[k]!! + 1.0) )
    }.sortedBy { x ->
      val (id, value) = x
      id.toInt()
    }.map { x ->
      val (id, value) = x
      "${id.toInt()}:${value}"
    }.joinToString(" ")
    val ans = if( stars.toDouble() > 4.0 ) 1 else if( stars.toDouble() <= 3.0 ) 0 else -1
    if( ans >= 0 ) {
      println("${ans} ${id_weight}")
    }
  }
}

fun weightChecker() {
  val term_index = File("./dataset/term_index.txt").readText().split("\n").filter { x ->
    x != ""
  }.map { x -> 
    val (term, index) = x.split(" ")
    Pair(index, term) 
  }.toMap()
  File("./dataset/svm.fmt.model").readText().split("\n").filter { x -> 
   x != "" 
  }.mapIndexed { i,x -> 
    //println("${i} ${x}")
    Pair(i - 6, x)
  }.filter { xs -> 
    val (i, x ) = xs
    i >= 0
  }.map { xs ->
    val (i, x ) = xs
    //println( "${term_index[i.toString()]}, ${x}" )
    Pair(term_index[(i+1).toString()], x.toDouble() )
  }.sortedBy{ xs ->
    val (term, weight) = xs
    weight
  }.map { xs ->
    val (term, weight) = xs
    println("${term} ${weight}")
  }
}
fun main(args:Array<String>) {
 val MODE = args.getOrElse(0) { "--termIndexer" } 
 when(MODE) { 
   "--termIndexer" -> termIndexer()
   "--svmIndexer"  -> svmIndexer()
   "--weightChecker"  -> weightChecker()
 }
}
