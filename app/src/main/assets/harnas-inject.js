(function () {
  'use strict';
  if (window.__ospInit) return;
  window.__ospInit = true;

  function flag(t) { try { document.title = t; } catch (e) {} try { OspAndroid.onFlag(t); } catch (e) {} }
  function done(p) { try { OspAndroid.onDone(p); } catch (e) {} }
  function quiz(q, a, s) { try { OspAndroid.onQuiz(q, a, s); } catch (e) {} }
  function txt() { return (document.body && document.body.innerText || '').toLowerCase(); }

  var email = window.__OSP_FORCE_EMAIL || '';

  var qs = location.search;
  if (/osp_wipe=1/.test(qs)) {
    var nx = (qs.match(/osp_next=([a-f0-9]{8})/) || [])[1];
    if (nx) { location.replace('https://osp-harnas.pl/zaglosuj-na-wybrane-osp/' + nx); return; }
  }

  var W = {
    'zero':0,'jeden':1,'dwa':2,'trzy':3,'cztery':4,'pięć':5,'piec':5,'sześć':6,'szesc':6,
    'siedem':7,'osiem':8,'dziewięć':9,'dziewiec':9,'dziesięć':10,'dziesiec':10,
    'jedenaście':11,'jedenascie':11,'dwanaście':12,'dwanascie':12,'trzynaście':13,'trzynascie':13,
    'czternaście':14,'czternascie':14,'piętnaście':15,'pietnascie':15,'szesnaście':16,'szesnascie':16,
    'siedemnaście':17,'siedemnascie':17,'osiemnaście':18,'osiemnascie':18,
    'dziewiętnaście':19,'dziewietnascie':19,'dwadzieścia':20,'dwadziescia':20,
    'trzydzieści':30,'trzydziesci':30,'czterdzieści':40,'czterdziesci':40,
    'pięćdziesiąt':50,'piecdziesiat':50,'sześćdziesiąt':60,'szescdziesiat':60,
    'siedemdziesiąt':70,'siedemdziesiat':70,'osiemdziesiąt':80,'osiemdziesiat':80,
    'dziewięćdziesiąt':90,'dziewiecdziesiat':90,'sto':100,'tysiąc':1000,'tysiac':1000
  };
  function num(t) { if (W[t] !== undefined) return W[t]; var n = parseInt(t, 10); return isNaN(n) ? null : n; }

  function solve(q) {
    if (!q) return null;
    var t = q.toLowerCase().trim().replace(/&quot;/g,'"').replace(/[„”"']/g,'').replace(/[?!.,]/g,'')
             .replace(/podzielone przez|dzielone przez/g,'/');
    if (/ćwiartek|cwiartek/.test(t)) return '4';
    if (/milion/.test(t) && /miliard/.test(t)) return '1000';
    if (/tuzin/.test(t) && /24/.test(t)) return '2';
    if (/zer ma liczba sto/.test(t)) return '2';
    if (/zer ma liczba tysi/.test(t)) return '3';
    if (/zer ma liczba milion/.test(t)) return '6';
    if (/tuzin to ile|ile sztuk to tuzin/.test(t)) return '12';
    if (/kopa to ile|ile sztuk to kopa/.test(t)) return '60';
    if (/boków ma trójkąt|bokow ma trojkat/.test(t)) return '3';
    if (/boków ma kwadrat|bokow ma kwadrat/.test(t)) return '4';
    if (/boków ma pięciokąt|bokow ma pieciokat/.test(t)) return '5';
    if (/boków ma sześciokąt|bokow ma szesciokat/.test(t)) return '6';
    if (/boków ma ośmiokąt|bokow ma osmiokat/.test(t)) return '8';
    if (/kół ma rower|kol ma rower/.test(t)) return '2';
    if (/pór roku|por roku/.test(t)) return '4';
    if (/palc/.test(t) && /obu|dwóch|dwoch/.test(t)) return '10';
    if (/palc/.test(t) && /dłoni|dloni/.test(t)) return '5';
    if (/dni ma tydzień|dni ma tydzien/.test(t)) return '7';
    if (/miesięcy ma rok|miesiecy ma rok/.test(t)) return '12';
    if (/godzin ma doba/.test(t) && /pół|pol/.test(t)) return '12';
    if (/godzin ma doba/.test(t)) return '24';
    if (/minut ma godzina/.test(t)) return '60';
    if (/centymetr/.test(t) && /metr/.test(t)) return '100';
    if (/połówek|polowek|połowek/.test(t)) return '2';
    if (/luty/.test(t) && /nieprzestępn|nieprzestepn/.test(t)) return '28';
    var sq = t.match(/kwadrat liczby\s*([a-ząćęłńóśźż0-9]+)/);
    if (sq) { var s1 = num(sq[1]); if (s1 !== null) return String(s1 * s1); }
    var lw = t.match(/ile liter ma s[łl]owo\s*([a-ząćęłńóśźż]+)/);
    if (lw) return String(lw[1].trim().length);
    var e = t.replace(/^ile (to jest|wynosi):?\s*/, '');
    var p = e.match(/[a-ząćęłńóśźż0-9]+|plus|\+|minus|-|razy|\*|\/|przez/g);
    if (p) {
      var parts = [], i;
      for (i = 0; i < p.length; i++) {
        var a = num(p[i]), b = i + 1 < p.length ? num(p[i + 1]) : null;
        if (a !== null && a >= 20 && a <= 90 && a % 10 === 0 && b !== null && b >= 1 && b <= 9) {
          parts.push(String(a + b)); i++;
        } else parts.push(p[i]);
      }
      if (parts.length >= 3) {
        var acc = num(parts[0]);
        if (acc !== null) {
          for (i = 1; i < parts.length - 1; i += 2) {
            var v = num(parts[i + 1]);
            if (v === null) { acc = null; break; }
            if (parts[i] === 'plus' || parts[i] === '+') acc += v;
            else if (parts[i] === 'minus' || parts[i] === '-') acc -= v;
            else if (parts[i] === 'razy' || parts[i] === '*') acc *= v;
            else if (parts[i] === '/' || parts[i] === 'przez') acc = v === 0 ? null : acc / v;
            else { acc = null; break; }
          }
          if (acc !== null) return String(acc);
        }
      }
    }
    return null;
  }

  function norm(s) {
    var m = {'ą':'a','ć':'c','ę':'e','ł':'l','ń':'n','ó':'o','ś':'s','ź':'z','ż':'z'};
    return (s || '').toLowerCase().replace(/[ąćęłńóśźż]/g, function (c) { return m[c]; }).trim();
  }

  function setVal(el, v) {
    if (!el || el.value === v) return;
    el.value = v;
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }

  function findOpt(sel, val, label) {
    var el = document.querySelector(sel);
    if (!el) return null;
    var byVal = el.querySelector('option[value="' + val + '"]');
    if (byVal) return byVal;
    var opts = el.querySelectorAll('option');
    for (var i = 0; i < opts.length; i++) if (norm(opts[i].textContent) === norm(label)) return opts[i];
    return null;
  }

  function setSel(sel, val, label) {
    var el = document.querySelector(sel);
    if (!el) return 'noel';
    var opt = findOpt(sel, val, label);
    if (!opt) return 'wait';
    if (el.value === opt.value) return 'ok';
    el.value = opt.value;
    el.dispatchEvent(new Event('change', { bubbles: true }));
    return 'set';
  }

  function switchOn(el) {
    if (!el) return;
    if (el.getAttribute('aria-checked') === 'true' || el.checked) return;
    el.click();
  }

  function token() {
    var f = document.querySelectorAll('input[name="cf-turnstile-response"], textarea[name="cf-turnstile-response"]');
    for (var i = 0; i < f.length; i++) if (f[i].value && f[i].value.length > 20) return f[i].value;
    return '';
  }

  function submit(form) {
    if (!form || window.__ospLock) return;
    window.__ospLock = true;
    var h = form.querySelector('input[name="website"]');
    if (h) h.value = '';
    try { HTMLFormElement.prototype.submit.call(form); }
    catch (e) { window.__ospLock = false; form.submit(); }
  }

  var vid = (location.pathname.match(/\/zaglosuj-na-wybrane-osp\/([a-f0-9]{8})/) || [])[1];
  if (vid) { try { localStorage.setItem('last_vote_id', vid); } catch (e) {} }

  var ticks = 0;
  var timer = setInterval(function () {
    if (++ticks > 900) { clearInterval(timer); return; }

    var body = txt();
    if (/429|too many requests|za dużo/i.test(body)) { flag('OSP-429'); return; }
    if (/nie znaleziono|404|nie istnieje/i.test(body)) { flag('OSP-MARTWY'); return; }
    if (/sesja wygasła|sesja wygasla|odśwież stronę/i.test(body)) {
      flag(location.pathname.indexOf('confirm') > -1 ? 'OSP-SESIA' : 'OSP-SESIA-VOTE'); return;
    }

    var form = document.getElementById('vote-form');
    if (form && document.getElementById('vote_email')) {
      flag('OSP-CZEKA-VOTE');
      if (email) setVal(document.getElementById('vote_email'), email);

      if (setSel('#vote_voivodeship', '4', 'wielkopolskie') !== 'ok') return;
      if (setSel('#vote_district', '1971', 'Żelazków') !== 'ok') return;
      if (setSel('#vote_city', '13879', 'Żelazków') !== 'ok') return;

      switchOn(document.getElementById('vote_agreement_1'));
      switchOn(document.getElementById('vote_agreement_2'));

      if (token()) { flag('OSP-OK-VOTE'); submit(form); done('vote'); }
      else flag('OSP-CZEKA-VOTE-BRAK');
      return;
    }

    var cf = document.querySelector('.c-vote-confirm__form');
    if (cf) {
      flag(vid ? 'OSP-CZEKA-' + vid : 'OSP-CZEKA');
      var ql = document.querySelector('.c-vote-confirm__question');
      var q = ql ? ql.textContent.trim() : '';
      var ans = solve(q);
      var inp = document.getElementById('answer');
      if (ans && inp && inp.value !== ans) setVal(inp, ans);
      quiz(q, ans || '', !!ans);
      if (token() && inp && inp.value) { flag('OSP-OK-CONF'); submit(cf); done('confirm'); }
      return;
    }

    if (vid) flag('OSP-CZEKA-' + vid);
  }, 700);
})();
