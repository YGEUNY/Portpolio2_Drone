<!DOCTYPE html>
<html>
<head>
  <title>DRONE111</title>
  <meta charset="utf-8">
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
</head>
<body>
  <?php
    $index = $_POST["index"];
   ?>
   <h1>분석 결과</h1>
  <div>
    <?php
      $i= 0;
      for($i=1; $i<=$index; $i++){
     ?>
    <button onclick="show_data(<?=$i?>)"><?=$i?></button>
  <?php } ?>
    <p id="result"></p>
  </div>
  <script>
    function show_data(index_number){
      $('#result').text("");
      var index = index_number;
      $.ajax({
      url : "/"+index+"/result.json",
      dataType : "json",
      success:function(data){
        for(var i=0; i<data.length;i++){
          console.log("\t아이디 : " + data[i].frame_id + ",   사진:  " + data[i].filename + ",   종류 : "+data[i].objects);

		      var name = (data[i].filename).split('/');
		      var image_name = name[2].split('.')

		      console.log("\t파일이름:"+name[2]);

		      var path = "<img src = '/"+index+"/"+image_name[0]+".jpg' width='300px' align='left' hspace='10px'>";
		      $('#result').append("<br><br>"+path);


          var len = data[i].objects;
          var can = 0, vinyl = 0, plastic=0, paper=0, rope=0, rubber=0;
          for(var j=0; j<len.length;j++){
              if(data[i].objects[j].name==can)  can++;
              else if(data[i].objects[j].name==vinyl) vinyl++;
              else if(data[i].objects[j].name==plastic) plastic++;
              else if(data[i].objects[j].name==paper) paper++;
              else if(data[i].objects[j].name==rope) rope++;
              else if(data[i].objects[j].name==rubber) rubber++;
              // $('#result').append("<div style='margin-left: 226px;'>Type : "+data[i].objects[j].name + "<br></div>");
          }
          if(can>0){
            $('#result').append("<div style='margin-left: 226px;'>Type : can"+can+ "개<br></div>");
          }else if(vinyl>0) {
            $('#result').append("<div style='margin-left: 226px;'>Type : "+vinyl+ "개<br></div>");
          }else if(plastic>0) {
            $('#result').append("<div style='margin-left: 226px;'>Type : "+plastic + "개<br></div>");
          }else if(paper>0) {
            $('#result').append("<div style='margin-left: 226px;'>Type : "+paper+ "개<br></div>");
          }else if(rope>0) {
            $('#result').append("<div style='margin-left: 226px;'>Type : "+rope+ "개<br></div>");
          }else if(rubber>0) {
            $('#result').append("<div style='margin-left: 226px;'>Type : "+rubber+ "개<br></div>");
          }
          $('#result').append("<br clear=left>");
        }
     }})}
  </script>
</body>
</html>​
