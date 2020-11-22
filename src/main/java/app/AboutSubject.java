package app;

import io.reactivex.subjects.*;

public class AboutSubject {

    public static void main(String[] args) {

        //самая простая реализация Subject
        System.out.println("publish subject");
        PublishSubject<Integer> publishSubject = PublishSubject.create();
        publishSubject.onNext(1);
        publishSubject.subscribe(System.out::println);
        publishSubject.onNext(2);
        publishSubject.onNext(3);
        publishSubject.onNext(4);

        //хранит только последнее значение
        System.out.println("\nbehaviour subject");
        BehaviorSubject<Integer> behaviourSubject = BehaviorSubject.create();
        behaviourSubject.onNext(0);
        behaviourSubject.onNext(1);
        behaviourSubject.onNext(2);
        behaviourSubject.subscribe(v -> System.out.println("Late: " + v));
        behaviourSubject.onNext(3);

        //имеет специальную возможность кэшировать все поступившие в него данные
        System.out.println("\nreplay subject");
        ReplaySubject<Integer> replaySubject = ReplaySubject.create();
        replaySubject.subscribe(v -> System.out.println("Early:" + v));
        replaySubject.onNext(0);
        replaySubject.onNext(1);
        replaySubject.subscribe(v -> System.out.println("Late: " + v));
        replaySubject.onNext(2);

        //хранит последнее значение
        System.out.println("\nasync subject");
        AsyncSubject<Integer> asyncSubject = AsyncSubject.create();
        asyncSubject.subscribe(v -> System.out.println(v));
        asyncSubject.onNext(0);
        asyncSubject.onNext(1);
        asyncSubject.onNext(2);
        asyncSubject.onComplete();
    }

}

