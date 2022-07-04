<!DOCTYPE html>
<html>
<head>
  <title>CONNECT</title>
  <meta charset="utf-8">
</head>
<body>
  <?php
   $conn = mysqli_connect("localhost","root","123456", "trash") or die("fail"); // DB ip주소, ID, pw, DB이름 => DB연결
   mysqli_set_charset($conn,"utf-8");

   /////////////////////목차개수 DB 가져오기///////////////
   $query = "SELECT * FROM trashindex ORDER BY id DESC LIMIT 1";
   $index_result = mysqli_query($conn, $query); //쿼리 보내고 결고 변수에 저장
   if(mysqli_num_rows($index_result)>0){//저장 데이터 있음
     $row = mysqli_fetch_assoc($index_result);
     $index = $row['trashNumber'];
   }else{//테이블에 데이터 없음
     $index = 0;
   }

   ////////// 안드로이드에서 전송된 값 받아서 DB에 저장///////
   $name = $_POST["name"];
   if(!empty($name)){//전송된 값이 있을 때
     if(!strcmp($name, "YANG")){  //같으면0(false)반환 다르면1(true)반환
       $index++;
       $sql = "insert into trashindex (id, trashname, trashNumber)
                    values(null, '$name', '$index')"; //trashindex = 테이블명,  괄호 안 = 해당 테이블의 필드들, => 각 테이블 순서에 따라 저장 요청 쿼리
       $result = mysqli_query($conn, $sql); //입력된 쿼리 결과 DBㅇㅔ 저장 & result변수에 저장
     }else{//값이 YANG가 아님
     }
   }else{//전송된 데이터 없음
   }

  //////////////////목차 만드는데 넘겨줄 인덱스 개수 db 저장///////////////
   $query = "SELECT * FROM trashindex ORDER BY id DESC LIMIT 1";
   $index_result = mysqli_query($conn, $query);
   $row = mysqli_fetch_assoc($index_result);
   $last_index = $row['id'];
   mysqli_close($conn);
   $_POST["index"] = $last_index;
   include('index_with_ajax.php');
?>
</body>
</html>
