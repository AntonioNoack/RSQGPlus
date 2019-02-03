
function request(url, args, callback){
	var x = new XMLHttpRequest();
	x.open("GET", url);
	x.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	x.onreadystatechange = function(){
		if(x.readyState==4 && x.status==200)
			callback(x.responseText);
	}
	x.send(args);
}

/*request('gp.html', '', x => {
	x.split('Antonio').forEach(y => {
		console.log(y.substring(0, 200));
	})
})*/

function atob2(s){
	s = atob(s);
	for(var i=0;i<s.length;i++){
		var c0 = s.charCodeAt(i);
		if(c0 > 127){
			var dif, code;
			if((c0 >> 5) == 6){
				var c1 = s.charCodeAt(i+1);
				code = (c0 & 31) * 64 + (c1 & 63);
				dif = 2;
			} else if((c0 >> 4) == 14){
				var c1 = s.charCodeAt(i+1);
				var c2 = s.charCodeAt(i+2);
				code = (c0 & 15) * 64 * 64 + (c1 & 63) * 64 + (c2 & 63);
				dif = 3;
			} else if((c0 >> 3) == 30){
				var c1 = s.charCodeAt(i+1);
				var c2 = s.charCodeAt(i+2);
				var c3 = s.charCodeAt(i+3);
				code = (c0 & 7) * 64 * 64 * 64 + (c1 & 63) * 64 * 64 + (c2 & 63) * 64 + (c3 & 63);
				dif = 4;
			} else return s;// error
			s = s.substr(0, i)+String.fromCharCode(code)+s.substr(i+dif);
		}
	};return s;
}

function toggleMenue(e){
	var that = this;
	if(that.prot) return;
	that.prot = 1;
	var fac = .8;
	var menu = document.getElementById('menu'),
		main = document.getElementById('main');
	if(menu.style.display.indexOf('none') > -1){
		// make visible :)
		main.style.width = '89%';
		main.style.left = '11%';
		menu.style['animation-name'] = 'goneLeftGoBack';
		menu.style['animation-duration'] = '1s';
		menu.style.display = '';
		setTimeout(() => {
			that.prot = 0;
		}, fac * 1000);
	} else {
		menu.style['animation-name'] = 'goneLeft';
		menu.style['animation-duration'] = '1s';
		setTimeout(() => {
			menu.style.display = 'none';
			main.style.width = '100%';
			main.style.left = '0%';
			that.prot = 0;
		}, fac * 1000);
	}
}

function c(n){
	return document.createElement(n);
}

function div(className = ''){
	var x = c('div');
	x.className = className;
	return x;
}

function a(className = '', href = ''){
	var x = c('a');
	x.className = className;
	x.href = href;
	return x;
}

var floor = Math.floor;
function setText(e, txt){
	// todo check whether 3 or more lines are needed...
	var split = txt.split('<br>');
	if(split.length > 3){
		var shorten = split[0]+'<br>'+split[1]+'<br>'+split[2];
		e.innerHTML = shorten;
		var expand = div('a');
		var expanded = 0;
		expand.innerText = '...';
		expand.onclick = function(){
			// todo merke dir alle Kinder nach mir
			var merk = [], i = 0;
			for(;i<e.children.length;i++){
				if(e.children[i] == expand){
					break;
				}
			}
			for(;i<e.children.length;i++){
				merk.push(e.children[i]);
			}
			if(expanded){
				e.innerHTML = shorten;
				merk.forEach(x => e.append(x));
			} else {
				e.innerHTML = txt;
				merk.forEach(x => e.append(x));
			};expanded = !expanded;
		}
		e.append(expand);
	} else {
		e.innerHTML = txt;
	}
}

var iFrame;
function createContent(parent, data){
	var container = div('e');
	var main = div('ec');
	container.append(main);
	var pic = c('img');
	pic.className = 'e_i';
	pic.src = '?pp='+data.c.p;
	var name = div('e_n');
	name.innerText = data.c.n;
	var time = div('e_d');
	time.innerText = dt(data.dc);
	main.append(pic);
	main.append(name);
	main.append(time);
	if(data.p){
		var party = div('e_n2');
		party.innerText = '('+data.p.substr(data.p.indexOf(',')+1)+')';
		main.append(party);
	}
	// todo urls...
	var txt = div('e_t');
	main.append(txt);
	var t = data.t;
	if(data.t.indexOf('<link>') > -1){
		t = data.t.split('<link>');
		var dat = t[1], dati = dat.indexOf('/');
		var title = atob2(dat.substr(0, dati)).split('\n').join('').split('\t').join('').trim();
		var url = dat.substr(dati+1);
		t = t[0];
		var ele = a('e_lk', url);
		ele.innerText = title;
		main.append(ele);
	}
	if(t.indexOf('<reshare>') > -1){
		t = t.split('<reshare>');
		var sub = div('rs');
		var num = t[1]*1;
		// todo load the entry from the server...
		request('?qp='+num, '', x => {
			analyse(x, () => sub);
		});
		if(t[0].length < 1){
			txt.style['padding-top'] = '0px';
		} else setText(txt, t[0]);
		txt.append(sub);
	} else {
		if(t.length < 1){
			txt.style['padding-top'] = '0px';
		} else setText(txt, t);
	}
	
	// todo multiple images...
	if(data.i[0].length){
		if(data.i[0]*1){
			var img = c('img');
			img.className = 'e_p';
			img.src = '?pcp='+data.i[0];
			main.append(img);
			img.onclick = function(){
				window.location.href = '?p='+data.id;
			}
			// todo onclick open div with the image in full size :)
		} else {
			// todo add the link to the video...
			var dat = atob(data.i[0]).split('///');
			var type = dat[0];
			var url = dat[1];
			var desc = dat[2];// todo wird irgendwie abgeschnitten :(
			if(type == 'video/*' && url.indexOf('youtube.com') > -1){
				var vwrap = c('div');
				vwrap.className = 'e_p';
				vwrap.style.height = '0';
				vwrap.style.position = 'relative';
				vwrap.style['padding-bottom'] = '75%';
				var x;// https://i.ytimg.com/vi/oyVWDFCH53o/hqdefault.jpg 
				var icon = div('e_v e_vx');
				var img = c('img');
				img.className = 'e_v';
				// video.className = 'e_v e_vx';
				var splitUrl = url.indexOf('v=') > -1 ? url.split('v=') : decodeURIComponent(url).split('v=');
				var code = splitUrl[1].split('&')[0];
				// video.src = 'https://www.youtube.com/embed/'+code;
				img.src = 'https://i.ytimg.com/vi/'+code+'/hqdefault.jpg';
				icon.onclick = function(){
					if(iFrame){
						if(iFrame.parent == vwrap) return;
						var par = iFrame.parentNode;
						par.removeChild(iFrame);
						par.children[0].style.display = '';
						par.children[1].style.display = '';
					} else {
						iFrame = c('iframe');
						iFrame.className = 'e_v';
						iFrame.setAttribute('allowFullScreen','');
					}
					
					iFrame.src = 'https://www.youtube.com/embed/'+code+'?autoplay=1';
					icon.style.display = 'none';
					// img.style.display = 'none';
					vwrap.append(iFrame);
					// todo reuse the old iframe...
					// todo show the iframe
				}
				// video.src = '';
				// video.style.background = 'url("img/youtubeOff.png")';
				vwrap.append(img);
				vwrap.append(icon);
				main.append(vwrap);
			} else {
				console.log({type,url,desc});
			}
		}
	}
	
	// todo if angemeldet Icon + "Kommentar hinzufügen"
	// todo onClick aufbloppen, um Hochladeoptionen anzuzeigen
	
	var inp = div('e_a');
	var inpTxt = c('input');
	var cmts = div();
	
	var id = data.id;
	cmts.onclick = function(){
		window.location = window.location + '?p='+id;
	}
	
	inpTxt.className = 'e_ai';
	inpTxt.placeholder = 'Kommentieren';
	inpTxt.type = 'text';
	inp.append(cmts);
	inp.append(inpTxt);
	main.append(inp);
	var lik = div('e_l');
	var likt = div('e_lt');
	lik.append(likt);
	likt.innerText = "+"+(data.l*1||'1');
	inp.append(lik);
	
	// comment section and likes
	// todo Aufklappoption if needed...
	
	parent.append(container);
	return function(data){
		var cmt = div('e_c');
		var body = div('e_c_c');
		var name = a('e_c_n', '?p='+data.c.i);
		name.innerText = data.c.n;
		var img = c('img');
		img.className = 'e_i';
		img.src = '?pp='+data.c.p;
		var time = div('e_c_d');
		time.innerText = dt(data.dc);
		var txt = div('e_c_t');
		txt.innerHTML = data.t;
		
		body.append(name);
		cmt.append(time);
		body.append(txt);
		cmt.append(img);
		cmt.append(body);
		cmts.insertBefore(cmt, cmts.firstChild);
	};
}

function dt(s){
	s = s * 1;
	if(s == -1) return '';
	if(s < 180){
		return s+'s';
	} else {
		s = ((s+30)/60)|0;
		if(s < 180){
			return s+'min';
		} else {
			s = ((s+30)/60)|0;
			if(s < 72){
				return s+'h';
			} else {
				s = ((s+12)/24)|0;
				if(s < 32){
					return s+'d';
				} else {
					s = ((s+3)/7)|0;
					if(s < 54){
						return s+'w';
					} else {
						s = ((s+26)/52.177)|0;
						return s+'y';
					}
				}
			}
		}
	}
}

var main = [1,2,3].map(x => document.getElementById('main'+x));

document.onscroll = function(){
	var min = Infinity;
	main.forEach(mn => {
		if(mn && mn.lastChild){
			min = Math.min(min, mn.lastChild.offsetTop);
		}
	});
	var delta = pageYOffset - min;
	if(delta > -innerHeight/2){
		req();
	}
};
var mainIndex = 0;
function pushMain(e){
	main[(mainIndex++)%3].append(e);
}

function ot(x){
	return (x=x.lastChild)?x.offsetTop:-1;
}

function getMain(){
	var x1 = ot(main1), x2 = ot(main2), x3 = ot(main3);
	if(x1 < x2 && x1 < x3) return main1;
	if(x2 < x3) return main2;
	return main3;
}

function analyse(x, getMain){
	var players = {};
	var thread;
	x.split(';').forEach(x => {
		var data = x.split(',');
		switch(data[0]){
		case '1':
			// todo create thing, register listeners and such... and the id and content of it :)
			var id = data[1];
			var creator = players[data[2]];
			var content = atob2(data[3]);
			var images = data[4].split('.');
			var party = atob2(data[5]);
			var dtCreated = data[6];
			var dtUpdated = data[7];
			var likes = data[8];// todo be able to see who liked it
			thread = createContent(getMain(), {id,c:creator,t:content,i:images,l:likes,p:party,dc:dtCreated,du:dtUpdated});
			break;
		case '2':// todo add comment
			var creator = players[data[2]];
			var content = atob2(data[3]);
			var images = data[4].split('.');
			var dtCreated = data[6];
			var dtUpdated = data[7];
			thread({c:creator,t:content,i:images,dc:dtCreated,du:dtUpdated});
			break;
		case '3':// todo add player data
			var id = data[1];
			var name = atob2(data[2]);
			var pic = data[3];
			players[id] = {n:name,p:pic,i:id};
			break;
		}
	});
	return thread;
}

var siteIndex = 0;
var requesting = 0;
function req(){
	if(requesting) return;
	requesting = 1;
	request('?qs='+siteIndex+++'&t='+(new Date().getTime()), '', x => {
		var thread = analyse(x, getMain);
		if(thread){
			setTimeout(() => {
				requesting = 0;
			}, 1000);
		}
	});
}

document.getElementsByTagName('bot')[0].innerHTML = '';

req(0);
// todo request if scrolled down completely...