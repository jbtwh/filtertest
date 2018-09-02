# filtertest
стартовая ф-ция: -main

веб-сервер слушает на 5000-м порту (http://localhost:5000/filter)

добавить сообщение вручную:
(.get (gregor/send (:producer @kafka) "books" "cisp test"))
