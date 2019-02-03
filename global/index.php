<?php
	
	// instructions are located at https://anionoa.phychi.com/+/howTo
	
	// todo for you fill in:
	$databaseIP = 'phychi.com';
	$databaseUsername = 'myself';
	$databasePassword = '123456';
	
	// you're done now with that step :)
	// don't change the following if you don't know what you're doing
	
	function connect(){
		global $databaseIP, $databaseUsername, $databasePassword;
		$conn = new mysqli($databaseIP, $databaseUsername, $databasePassword, $databaseUsername);
		if ($conn->connect_error) {die('Database offline. '.$conn->connect_error);}
		if(!$conn->set_charset('utf8mb4')){die('Charset illegal. '.$conn->error);}
		return $conn;
	}
	
	$db = connect();
	
	function textPreview($txt, $len){
		while(1){
			$i = strpos($txt, '<');
			if($i === False) break;
			$j = strpos($txt, '>', $i+1);
			if($j === False) break;
			$txt = substr($txt, 0, $i).' '.substr($txt, $j+1);
		}
		$l = strlen($txt);
		if($l < $len){
			return $txt;
		} else {
			$j = strpos($txt, ' ', $len);
			if($j === False) return $txt;
			return substr($txt, 0, $j).'...';
		}
	}
	
	function showDefault(){
	
		global $db;
		$data = file_get_contents('template.html');
		$isBot = isBot();
		if($isBot){
		
			$i = strpos($data, '<bot>');
			
			echo substr($data, 0, $i+5);
			$res = $db->query('SELECT ID,Content FROM Threads WHERE Creator=\'1\' AND Party>-1 AND Flags=0 ORDER BY Created DESC');
			while($row = $res->fetch_assoc()){
				echo '<a href="?p='.$row['ID'].'">'.textPreview($row['Content'], 50).'</a>';
			}
			echo substr($data, $i+5);
			
		} else {
		
			echo $data;
			
		}
	}
	
	function endsWith($haystack, $needle){
		$length = strlen($needle);
		if($length == 0){
			return true;
		} else return (substr($haystack, -$length) === $needle);
	}
	
	function dt($name){
		return 'TIME_TO_SEC(TIMEDIFF(NOW(),'.$name.'))';
	}
	
	function download($url){
		if(strpos($url, '//') === 0){
			$url = 'https:'.$url;
		}
		$opts = array('https' =>
			array(
				'method'  => 'GET',
				'header'  => 'Content-type: application/x-www-form-urlencoded'//,'content' => $postdata
			));
		$context = stream_context_create($opts);
		$result = file_get_contents($url, false, $context);
		return $result;
	}
	
	function createThumbnail($data){
		set_time_limit(30);
		$image = imagecreatefromstring($data);
		$w = imagesx($image);
		$h = imagesy($image);
		$bg = imagecreatetruecolor($w, $h);
		imagefill($bg, 0, 0, imagecolorallocate($bg, 255, 255, 255));
		imagealphablending($bg, TRUE);
		imagecopy($bg, $image, 0, 0, 0, 0, $w, $h);
		imagedestroy($image);
		$quality = 95;
		do {
			ob_start();
			imagejpeg($bg, NULL, $quality);
			$newData = ob_get_clean();
			$quality -= 5;
		} while($quality >= 5 && strlen($newData) > 65535);
		imagedestroy($bg);
		return $newData;
	}
	
	function uploadMedia($db, $media){
		
		if(!$media) die('no media');
		
		$desc = $media->description;
		if(!$desc) $desc = '';
		
		$res = $db->query('SELECT UUID FROM Images WHERE Description LIKE \''.$db->real_escape_string('#img,'.$media->resourceName).',%\'');
		if($row = $res->fetch_assoc()){
			return $row['UUID'];
		}
		
		$data = download($media->url);
		if(strlen($data) > 32000){
			$tumb = createThumbnail($data);
			$db->query('INSERT INTO Images (Creator,Flags,Description,Data,Data2) VALUES (\'1\',\'64\',\''.$db->real_escape_string('#img,'.$media->resourceName.','.$desc).'\',\''.$db->real_escape_string($tumb).'\',\''.$db->real_escape_string($data).'\')');
		} else {
			$db->query('INSERT INTO Images (Creator,Flags,Description,Data2) VALUES (\'1\',\'64\',\''.$db->real_escape_string('#img,'.$media->resourceName.','.$desc).'\',\''.$db->real_escape_string($data).'\')');
		}
		
		return $db->insert_id;
	}
	
	function genName($name){
		$nme = '';
		$len = strlen($name);
		$space = 0;
		for($i=0;$i<$len;$i++){
			$char = $name[$i];
			if(($char >= 'A' && $char <= 'Z') || ($char >= '0' && $char <= '9') || ($char >= 'a' && $char <= 'z')){
				if($space){
					$space = 0;
					$nme .= strtoupper($char);
				} else {
					$nme .= $char;
				}
			} else if($char == ' '){
				$space = 1;
			}
		};return $nme;
	}
	
	function register($db, $plus){
	
		$name = $plus->displayName;
		$pic = $plus->avatarImageUrl;
		$uuid = $plus->resourceName;// users/11032035465
		$qr = $db->query('SELECT UUID FROM Players WHERE Info LIKE \''.$db->real_escape_string($uuid).',%\' LIMIT 1');
		if($row = $qr->fetch_assoc()){
			$id = $row['UUID'];
		} else {
			set_time_limit(10);
			// insert the user into the db...
			$nme = genName($name);
			if($pic){
				$image = download($pic);
				$db->query('INSERT INTO Images (Creator,Flags,Description,Data) VALUES (\'1\',\'32\',\''.$db->real_escape_string('#ppic,'.$name).'\',\''.$db->real_escape_string($image).'\')');
				$picId = $db->insert_id;
			} else {
				$picId = 0;
			}
			$db->query('INSERT INTO Players (Name,Flags,Info,Icon) VALUES (\''.$db->real_escape_string($nme).'\',\'1\',\''.$db->real_escape_string($uuid).','.$db->real_escape_string($name).'\',\''.$picId.'\')');
			$id = $db->insert_id;
		}
		return $id;
	}
	
	function catchVideos($db, $media){
		if(strpos($media->contentType, 'image/') === 0){
			return uploadMedia($db, $media);
		} else {
			return base64_encode($media->contentType.'///'.$media->url.'///'.$media->description);
		}
	}
	
	function uploadPost($db, $data){
		
		$author = register($db, $data->author);
		$created = $data->creationTime;
		$updated = $data->updateTime;
		$text = $data->content;
		$plusses = $data->plusOnes;
			
		if($data->link){
			$text .= '<link>'.base64_encode($data->link->title).'/'.$data->link->url;
		}
		
		$found = $db->query('SELECT ID FROM Threads WHERE Created=\''.$db->real_escape_string($created).'\'');
		if($found->num_rows > 0){
			$id = $found->fetch_assoc()['ID'];
			echo $id."\n";
			return $id;
		}
		
		if($data->resharedPost){
			$thatId = uploadPost($db, $data->resharedPost);
			$text .= '<reshare>'.$thatId;
		}
		
		$party = 0;
		if($data->postAcl && $data->postAcl->communityAcl){
			$comm = $data->postAcl->communityAcl->community;
			$uuid = $comm->resourceName;
			$name = $comm->displayName;
			$nme = genName($name);
			// todo insert into players if not existent :)
			$qr = $db->query('SELECT UUID FROM Players WHERE Info LIKE \''.$db->real_escape_string($uuid).',%\' LIMIT 1');
			if($row = $qr->fetch_assoc()){
				$party = $row['UUID'];// found :)
			} else {
				$db->query('INSERT INTO Players (Name,Flags,Info) VALUES (\''.$db->real_escape_string($nme).'\',\'2\',\''.$db->real_escape_string($uuid).','.$db->real_escape_string($name).'\')');
				$party = $db->insert_id;
			}
		}
		
		$imgs = 0;
		if($data->album){
			$media = $data->album->media;
			foreach($media as $medium){
				$id = catchVideos($db, $medium);
				if($imgs){
					$imgs .= ','.$id;
				} else {
					$imgs = $id;
				}
			}
		} else if(isset($data->media)){
			$imgs = catchVideos($db, $data->media);
		} else $imgs = '';
		
		print_r($data);
		
		$postId = $db->query('SELECT MAX(ID) AS X FROM Threads')->fetch_assoc()['X'] + 1;
		$db->query('INSERT INTO Threads (ID,Creator,Content,Images,Party,Created,Updated) VALUES (\''.$postId.'\',\''.$author.'\',\''.$db->real_escape_string($text).'\',\''.$db->real_escape_string($imgs).'\',\''.$party.'\',\''.$created.'\',\''.$updated.'\')');
		
		if($data->comments){
			foreach($data->comments as $comment){
				$crea = $db->real_escape_string($comment->creationTime);
				$auth = register($db, $comment->author);
				$ctnt = $comment->content;
				$db->query('INSERT INTO Threads (ID,Creator,Content,Flags,Party,Created,Updated) VALUES (\''.$postId.'\',\''.$auth.'\',\''.$db->real_escape_string($ctnt).'\',\'2\',\''.$party.'\',\''.$crea.'\',\''.$crea.'\')');
			}
		}
		
		if($plusses){
			foreach($plusses as $plus){
				$plus = $plus->plusOner;
				$id = register($db, $plus);
				$db->query('INSERT INTO Threads (ID,Creator,Flags,Party) VALUES (\''.$postId.'\',\''.$id.'\',\'1\',\''.$party.'\')');
			}
		}
		
		return $postId;
	}
	
	function uploadJSONPost($db, $file){
		if(endsWith($file, '.json')){
			$data = json_decode(file_get_contents($file));
			uploadPost($db, $data);
		}
	}
	
	function formatTime($time){
		global $now;
		$theTime = strtotime($time);
		if($theTime > 100) return $now - $theTime;
		else return -1;
	}
	
	function printEntry($db, $entry, &$map, $fi,$lk){
		$creator = $entry['Creator'];
		if($map[$creator] != 1){
			$map[$creator] = 1;
			$pl = $db->query('SELECT Name,Info,Icon FROM Players WHERE UUID=\''.intval($creator).'\'')->fetch_assoc();
			echo '3,'.$creator.','.base64_encode($pl['Name'] == '#auto' ? $pl['Info'] : $pl['Name']).','.$pl['Icon'].';';
		}
		$party = $fi ? $entry['Party'] : 0;
		if($party && !$map[$party]){
			$pl = $db->query('SELECT Name,Info,Icon FROM Players WHERE UUID=\''.intval($party).'\'')->fetch_assoc();
			$map[$party] = $pl['Info'];
		}
		echo ($fi?'1,':'2,').$fi.','.$entry['Creator'].','.base64_encode($entry['Content']).','.str_replace(',', '.', $entry['Images']).','.($fi && $party ? base64_encode($map[$party]) : '').','.formatTime($entry['Created']).','.formatTime($entry['Updated']).($fi?','.$lk.';':';');
	}
	
	function printEntryBot($db, $entry, &$map, $fi,$lk){
		
		$creator = $entry['Creator'];
		$ctx = $entry['Content'];
		$p = strpos($ctx, '<link>');
		
		if($p !== False){
			echo '<h2>'.substr($ctx, 0, $p);
			$p2 = strpos($ctx, '/', $p+4);
			echo '<br><a href="'.substr($ctx, $p2+1).'">'.base64_decode(substr($ctx, $p+6, $p2-$p-6)).'</a>';
			echo '</h2>';
		} else {
			echo '<h2>'.$ctx.'</h2>';
		}
		
		$images = $entry['Images'];
		$arr = explode(',', $images);
		
		if(ctype_digit($arr[0])){// all digits
			$l = count($arr);
			for($i=0;$i<$l;$i++){
				if(strlen($arr[$i]) > 0){
					echo '<img src="?pc='.$arr[$i].'">';
				}
			}
		} else {
			$dat = base64_decode($images);
			if(strpos($dat, 'video/*///') === 0){
				$dat = explode('///', $dat);
				echo '<a href="'.$dat[1].'"><b>'.htmlentities($dat[2]).'</b></a>';
			} // else unknown
		}
		
		if($map[$creator] != 1){
			$map[$creator] = 1;
			$pl = $db->query('SELECT Name,Info,Icon FROM Players WHERE UUID=\''.intval($creator).'\'')->fetch_assoc();
			echo '<p>'.(time()-strtotime($entry['Created'])).'s ago, by <b>'.htmlentities($pl['Name'] == '#auto' ? $pl['Info'] : $pl['Name']).'</b>'.($pl['Icon'] > 0 ? ' <img src="?pp='.$pl['Icon'].'">':'').'</p>';
		}
	}
	
	function isBot(){
		$ua = $_SERVER['HTTP_USER_AGENT'];
		return stripos($ua, 'bot') !== False || stripos($ua, 'spider') !== False;
	}
	
	function printEntries($db, $id, $max, $bot){
		global $now;
		$now = time();
		$select = 'SELECT Creator,Content,Images,Party,Created,Updated FROM Threads WHERE ID=\''.$id.'\' AND Party>-1 AND Flags=';
		$likes = $db->query('SELECT COUNT(*) AS Likes FROM Threads WHERE ID=\''.$id.'\' AND Flags=1')->fetch_assoc()['Likes'];
		$e1 = $db->query($select.'0 ORDER BY Created DESC LIMIT 1');
		$e2 = $db->query($select.'2 ORDER BY Created DESC LIMIT '.$max);
		if($e1 = $e1->fetch_assoc()){
			if($bot){
				printEntryBot($db, $e1, $map, $id, $likes);
				while($er = $e2->fetch_assoc()){
					printEntryBot($db, $er, $map, 0, 0);
				}
			} else {
				printEntry($db, $e1, $map, $id, $likes);
				while($er = $e2->fetch_assoc()){
					printEntry($db, $er, $map, 0, 0);
				}
			}
		}
	}
	
	function createPreview($image){
		
		$w = imagesx($image);
		$h = imagesy($image);
		
		$maxWidth = 768;
		if($w > $maxWidth){
			// scaling needed
			$newHeight = $h * $maxWidth / $w;
			imagesetinterpolation($image, IMG_BICUBIC_FIXED);
			$newImage = imagescale($image, $maxWidth, $newHeight, IMG_BICUBIC_FIXED);
			imagedestroy($image);
			$sharpf = .2;
			$sharpen = array(
				array(0, -$sharpf, 0),
				array(-$sharpf, 1 + 4 * $sharpf, -$sharpf),
				array(0, -$sharpf, 0));
			$div = 1;
			imageconvolution($newImage, $sharpen, $div, 0);
			$image = $newImage;
			$w = $maxWidth;
			$h = $newHeight;
		}
		
		$nh = $h;
		
		$maxRatio = 1;
		if($h > $w * $maxRatio){// cut top & bottom
			$nh = $w * $maxRatio;
		}
		
		$bg = imagecreatetruecolor($w, $nh);
		imagefill($bg, 0, 0, imagecolorallocate($bg, 255, 255, 255));
		imagealphablending($bg, TRUE);
		imagecopy($bg, $image, 0, 0, 0, ($h-$nh) / 2, $w, $nh);
		imagedestroy($image);
		
		if($nh != $h){
			imagefilledpolygon($bg, array(0, $nh, $w/3, $nh, 0, $nh-$w/3), 3, 0);
		}
		
		return $bg;
	}
	
	if(isset($_GET['pp'])){// profile picture
		$id = intval($_GET['pp']);
		$res = $db->query('SELECT Data FROM Images WHERE UUID=\''.$id.'\' AND Flags=32');
		if($row = $res->fetch_assoc()){
			header('Content-Type: image/jpeg');
			header('Cache-Control: max-age=3000000');
			echo $row['Data'];
		} else {
			header('Location: img/no.pp.png', true, 302);
			exit();
		}
	} else if(isset($_GET['pcp'])){// posted picture preview
		$id = intval($_GET['pcp']);
		$res = $db->query('SELECT Data FROM Images WHERE UUID=\''.$id.'\' AND Flags=64');
		if($row = $res->fetch_assoc()){
			if(strlen($row['Data']) < 5){
				$row = $db->query('SELECT Data2 AS Data FROM Images WHERE UUID=\''.$id.'\' AND Flags=64')->fetch_assoc();}
			header('Content-Type: image/jpeg');
			header('Cache-Control: max-age=3000000');
			echo $row['Data'];
		} else {
			header('Location: img/404.jpg', true, 302);
			exit();
		}
	} else if(isset($_GET['pc'])){// posted picture
		$id = intval($_GET['pc']);
		$res = $db->query('SELECT Data2 FROM Images WHERE UUID=\''.$id.'\' AND Flags=64');
		if($row = $res->fetch_assoc()){
			header('Content-Type: image/jpeg');
			header('Cache-Control: max-age=3000000');
			echo $row['Data2'];
		} else {
			header('Location: img/404.jpg', true, 302);
			exit();
		}
	} else if(isset($_GET['ps'])){// posted picture preview
		$id = intval($_GET['ps']);
		$res = $db->query('SELECT Data2 FROM Images WHERE UUID=\''.$id.'\' AND Flags=64');
		if($row = $res->fetch_assoc()){
		
			header('Content-Type: image/jpeg');
			// header('Cache-Control: max-age=3000000');
			
			$data = $row['Data2'];
			$image = imagecreatefromstring($data);
			$bg = createPreview($image);
			$quality = 95;
			imagejpeg($bg, NULL, $quality);
			imagedestroy($bg);
			
		} else {
			header('Location: img/404.jpg', true, 302);
			exit();
		}
	} else if(isset($_GET['qs'])){
		$perSite = 24;
		$site = intval($_GET['qs']);
		$offset = $site * $perSite;
		$res = $db->query('SELECT DISTINCT ID FROM Threads WHERE Creator=\'1\' AND Party>-1 AND Flags=0 ORDER BY Created DESC LIMIT '.$perSite.' OFFSET '.$offset.'');
		$map = array();
		while($row = $res->fetch_assoc()){
			$id = $row['ID'];
			printEntries($db, $id, 50, false);
		}
	} else if(isset($_GET['qp'])){
		
		$id = intval($_GET['qp']);
		printEntries($db, $id, 50, false);
		
	} else if(isset($_GET['x'])){
		// upload the missing picture...
		$media = new stdClass;
		$media->resourceName = 'media/CixBRjFRaXBOYkotMXhIQkxoNnFPSjF2TlZQc2xFWXljZDNjRHV3alNXMUtjZg==';// the resource name from the json
		$media->url = 'http://localhost/anionoa/plus/img/1293.png';// your image path of the downloaded image
		echo uploadMedia($db, $media);
	} else if(isset($_GET['i'])){
		
		// #2
		// EN: disable this part in the final version by commenting it out or deleting it
		// (commenting out = make it so, that the server doesn't execute it, e.g. with // infront of every line affected)
		// DE: deaktiviere diesen Teil in der Version, die du ins Netz stellt, damit niemand diese Funktion misbraucht
		// z.B. mit diesen Strichen // vor den Zeilen, die nicht mehr gebraucht werden (alles hier in diesem {...}-Bereich, inklusive dem isset($_GET['i']))
		
		// your folder with the downloaded Takeout data
		// dein Ordner mit den heruntergeladenen Daten: (bei Linux beginnen Pfade mit / statt mit C:/, ist aber prinzipiell sehr ähnlich)
		$folder = '/home/antonio/Downloads/tske/Takeout/str/Beiträge/';
		$i = 0;
		$s = 0;// Start
		$e = 1;// Ende/end
		foreach(scandir($folder) as $file){
			$i++;
			if($i >= $s){
				set_time_limit(10);
				if($i > $e) break;
				uploadJSONPost($db, $folder.$file);
			}
		}
	} else if(isset($_GET['p'])){
		
		$id = intval($_GET['p']);
		
		$isBot = isBot();
		if($isBot){
		
			ob_start();
		
			printEntries($db, $id, 35, true);
			
			$postData = ob_get_clean();
			
			// #3 wenn du das Icon ändern möchtest, ändere hier https://phychi.com/img/fav... in deine entsprechenden URLs um
			
			if(strlen($postData) > 0){
				echo '<html><head>
	<meta charset="UTF-8"/>
	<title>'.htmlentities(textPreview($e1['Content'], 50)).'</title>
	<meta name="viewport" content="width=1024" initial-scale="2.0">
	<meta name="apple-mobile-web-app-capable" content="yes">
	<meta name="mobile-mobile-web-app-capable" content="yes">
	<link rel="apple-touch-icon" sizes="144x144" href="https://phychi.com/img/apple-touch-icon.png">
	<link rel="icon" type="image/png" href="https://phychi.com/img/fav128.png" sizes="128x128">
	<link rel="icon" type="image/png" href="https://phychi.com/img/fav64.png" sizes="64x64">
	<link rel="icon" type="image/png" href="https://phychi.com/img/fav32.png" sizes="32x32">
	<link rel="icon" type="image/png" href="https://phychi.com/img/fav16.png" sizes="16x16">
	<meta name="theme-color" content="#f5801f">
	<meta name="author" content="Antonio Noack"/>
</head>';
				echo $postData;
				echo '</body></html>';
			}
			
		} else {
		
			ob_start();
		
			printEntries($db, $id, 500, false);
			
			$postData = ob_get_clean();
			
			$data = file_get_contents('template.html');
			$i = strpos($data, '<bot></bot>');
			echo substr($data, 0, $i);
			echo '<data>'.$postData.'</data>';
			$j = strpos($data, 'main.js', $i);
			echo substr($data, $i+11, $j-$i-11);
			echo 'main1.js';
			echo substr($data, $j+7);
			
		}
		
	} else {
		showDefault();
	}
	
	$db->close();
	
?>