# 感情ではない商品評価極性辞書の構築

## 極性辞書は別に感情だけじゃない（と思う）
　 極性のpolarityという意味で軸を対象にネガティブとポジティブが存在するものをさすそうです。
　よく感情をガロア理論のように何かしらの対象構造を取れるとする主張も多いのですが、わたしはこの主張に対して少し懐疑的で、果たして楽しいの反対は悲しいなのか、無数の軸がある中でどうしてそれを対称だと思ったのかなどなど色々疑問点はあります。

　すでに東北大学の乾研究室さまが、感情に関する極性に関してはプロフェッショナルであり、たまに彼らの研究成果を後追いしているレベルです。

　さて、多くの研究では最初に極性の辞書を主観評価で決定します。これは、主に何を持って悪感情か、嬉しい感情なのか不明なため人間が定義してやる必要があるのですが、ここに主観が混じるので、評価者の人間の判断に委ねられるという側面があります。

　機械学習らしく、データ量で押し切ってしまうことで、もっと簡単に文章の極性が取れるものがあるので今回ご紹介します。

## Amazon, 楽天などの星は一次元情報であり、極性を構築するのに最適

　商品やサービスを気に入った場合には星が多く付き、気に入らなかったら少なくなるという単純な関係が成立しています。  
```sh
　気に入った＝好き、好意的
　気に入らなかった＝嫌い、嫌
```
 という仮定が入っていることにご注意してください。

 ということもあって、星情報は一次元で仮定を起きやすくやりやすいデータセットであります。

## 星の分布について
　以前はAmazonで簡単な評価を行ったことがありますが、今回は楽天のデータセットについて行いました。  
　楽天のデータセットは商品をクローリングしたものを20GByte超えのHTMLファイル（ただし、レビューは200MByte程度）を利用しました。  
　単純に星5個と、星1個を比較するのが理想なのですが、残念ながらひどい非対称データとなってしまいます。そのため、星5個 vs 星3,2,1個とします。    

## データの公開  
　最近クローリングに関しての倫理やコンプライアンスなどを読んでいると、クロールしたでデータの公開は問題ないように思えます[1,2]。[ここからダウンロード](https://www.dropbox.com/sh/xgvu1oouz8xduml/AABfFAxUO66wSqeGXkghlelIa?dl=0)できるようにしておきます。  
　クローラは先日公開したKotlinの実装のものを利用しました。リミットを外して利用すると、非常に高負荷になるので、秒間1アクセス程度がやはり限度のようです。  
　なお、このようにして集めたデータに関しては商用利用は原則ダメなようです。  

## Polarity(極性)
　ここには一部のみ記します。GISTには全て記すので、必要に応じて参照してください。     
気に入らないに入る TOP10
　[ここから全体を参照](https://gist.githubusercontent.com/GINK03/22c6da1e13640fcc538617863a8b3ec6/raw/929d8345852d2caec392b10fd00376e6d37a0398/polarity)できます。
```sh
ダイエー -4.211883314885654
がっかり -3.724952240087231
最悪 -3.629671589687795
二度と -3.615183142062377
期待はずれ -3.364096361814979
在庫管理 -3.251811276002615
シーブリーズ -3.243134607447971
返金 -3.223751242139063
江尾 -3.142244830633572
お蔵入り -3.044963500843487
...
```
気にいるに入る TOP 10
```
本当にありがとうございました 2.330683071541743
幸せ 2.40608193615266
閉店セール 2.415456005367995
強いて 2.425465450266797
増し 2.622845298273817
5つ 2.628383278795989
モヤモヤ 2.637474892812968
ドキドキ 2.759164930673644
しいて 3.162614441418143
迫る 3.249573225146807
```

## 極性の計算の仕方
　極性の計算は割と簡単にできて、全ての単語のlog(出現頻度+1)\*weightの合計値  0を下回ると否定的、0を上回ると肯定的です。  
  式にするとこのようなものになります。  
<p align="center">
  <img width="300" src="https://cloud.githubusercontent.com/assets/4949982/25076996/d8568b24-2360-11e7-9fa6-94713001c261.png">
</p>
　さらに、確率表現とするとこのようなものになります。  
<p align="center"> 
  <img width="300" src="https://cloud.githubusercontent.com/assets/4949982/25076997/db7bec4a-2360-11e7-8a4d-972d4978f19b.png">
</p>

# step by step. 機械学習
## step. 0 レビューデータと星数を特定のフォーマットで出力
　ダウンロードしたレビューのデータに対して、レビューデータとその時の星の数を抜き出して、あらかじめ決めたフォーマットで出力していきます。このフォーマットの形式をいかにちゃんと設計できるかも、技量だと思うのですが、ぼくはへぼいです。    
```sh
{星の数} セバレータ { レビューコンテンツ }
{星の数} セバレータ { レビューコンテンツ }
….
```
 kotlinで書くとこんな感じです。
```kotlin
fun rakuten_reviews(args: Array<String>) { 
  Files.newDirectoryStream(Paths.get("./out"), "*").map { name ->
    if( name.toString().contains("review") ) { 
      val doc = Jsoup.parse(File(name.toString()), "UTF-8")
      doc.select(".revRvwUserMain").map { x ->
        val star    = x.select(".revUserRvwerNum").text()
        val comment = x.select(".revRvwUserEntryCmt").text().replace("\n", " ")
        println("${star} __SEP__ ${comment}")
      }
    }
  }
}
```
## step. 1 単語のindex付け
　機械学習で扱えるように、単語にIDを振っていきます。深層学習のチュートリアルでよくあるものですが、私がよく使う方法を記します。    
　なお、今回は、Javaを使えるけどPythonは無理という方も多く、言語としての再利用性もJVM系の方が高いということで、Kotlinによる実装です。  
```kotlin
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
```

## step. 2 libsvmフォーマット化
　高次元データの場合、スパースで大規模なものになりやすく、この場合、Pythonなどのラッパー経由だと正しく処理できないことがあります。そのため、libsvm形式と呼ばれる形式に変換して扱います。    
　直接、バイナリに投入した方が早いので、以下の形式に変換します。  
```sh
1 1:0.12 2:0.4 3:0.1 ….
0 2:0.59 4:0.1 5:0.01 ...
```
　Kotlinで書くとこんな感じ
```kotlin
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
```

## step. 3 機械学習アルゴリズムにかけて、学習する
　学習アルゴリズムは割となんでもいいと思っているのですが、この前にQiitaで公開したデータに対して、素性の重要度の見方を書いてなかったので、重要度の確認の方法も兼ねて、liblinearで学習してみます。  
```sh
$ ./bin/train -s 0 ./dataset/svm.fmt
```
　さて、これでsvm.fmt.modelというファイルができます。このファイルの中のデータが、素性の重要度と対応しておりこのようなフォーマットになっています。  
```sh
solver_type L2R_LR
nr_class 2
label 1 0
nr_feature 133357
bias -1
w
-0.1026207840831818 
0.01714376979176739
....
```
-0.10\~\~という表記が、1ばんめの素性の重要度で、マイナスについていることがわかります。

## step. 4 学習結果と、単語idを衝突させる
　単純に重みのみ書いてあるとよくわからないので、idと重みを対応づけて、わかりやすく変形します。  
```kotlin
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
```

## 参考文献
[1] http://qiita.com/nezuq/items/c5e827e1827e7cb29011  
[2] http://q.hatena.ne.jp/1282975282  
