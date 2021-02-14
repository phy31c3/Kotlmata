
# Kotlmata

Automata-based programming library for Kotlin.

# Installation

### Gradle

```
implementation 'kr.co.plasticcity:kotlmata:1.0.3'
```

### Maven

```
<dependency>
  <groupId>kr.co.plasticcity</groupId>
  <artifactId>kotlmata</artifactId>
  <version>1.0.3</version>
  <type>pom</type>
</dependency>
```

# How to use

## KotlmataMachine

### 머신 생성

#### 기본형
```kotlin
val machine = KotlmataMachine("sample"/* 로그에 출력되는 머신의 이름 */, 2/* 로그레벨 */) {
    // 머신 정의
}
```
#### Lazy형
```kotlin
val machine by KotlmataMachine.lazy("sample") {
    // 머신 정의
}
```
#### 미리 정의된 머신이 있는 경우
```kotlin
val define: MachineDefine = {
    // 머신 정의
}
val machine = KotlmataMachine("sample") by define
```
#### 미리 정의된 템플릿이 있는 경우
```kotlin
val template: MachineTemplate = {
    // 머신 정의
    // 시작 상태는 정의할 수 없음
}
val machine = KotlmataMachine("sample") extends template by {
    // 추가 머신 정의
}
```

### 머신 정의

#### 상태 추가 및 시작 상태 지정
```kotlin
val machine = KotlmataMachine("sample") {
    "A" /* 상태의 태그(식별자) */ { state /* 상태 태그는 파라미터를 통해 얻을 수 있음 */ ->
        // 상태 정의
    } // 머신에 "A" 상태가 추가(생성) 됨
    10 /* 상태 태그는 어떤 타입으로 해도 상관 없음 */ {
        // 상태 정의
    }
    10 {
        // 동일한 태그의 상태를 중복해서 정의하면 기존 정의를 덮어씀
    }
    
    start at "A" // 머신의 시작 상태를 "A"로 지정
}
```
#### 전이규칙 정의 1 - 기본
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {} // 상태 "A" 추가
    "B" {} // 상태 "B" 추가
    
    "A" x "S" %= "B" // "A" 상태일 때 "S"이라는 신호가 입력될 경우 "B" 상태로 전이
    "A" x 10 %= "B" // 신호는 어떤 타입도 가능
    "A" x 10 %= "A" // 재귀도 가능함
    "A" x String::class %= "B" // String 타입의 객체가 신호로 입력되면 "B"로 전이 (타입 신호)
    "A" x String::class %= "A" // 동일한 좌변을 중복해서 정의하면 기존 규칙을 덮어씀

    start at "A"
}
```
#### 전이규칙 정의 2 - 키워드
```kotlin
"A" x "S" %= self // "A" x "S" %= "A"와 같음 (재귀)
"A" x "S" %= stay // "A" 상태일 때 "S"가 입력되면 현재 상태 유지 (재귀와 다르게 전이가 발생하지 않음)
"A" x any %= "B" // "A" 상태일 때 아무 신호나 입력되면 "B"로 전이
any x "S" %= "A" // 어떤 상태든지 "S"가 입력되면 "A"로 전이
any x "S" %= self // 어떤 상태든지 "S"가 입력되면 재귀
any x "S" %= stay // 어떤 상태든지 "S"가 입력되면 현재 상태 유지
```
#### 전이규칙 정의 3 - 다중 상태/신호 정의
```kotlin
("A" AND "B") x "S" %= "C" // "A" 또는 "B" 상태일 때 "S"가 입력되면 "C"로 전이
"A" x ("S" OR 10) %= "B" // "A" 상태일 때 "S" 또는 10이 입력되면 "B"로 전이
```
#### 전이규칙 정의 4 - 제외 상태/신호 정의
```kotlin
any("A", "B") x (10 OR 20) %= "C" // "A"나 "B" 상태가 아닐 때 10 또는 20이 입력되면 "C"로 전이
("A" AND "B") x any(10, 20) %= "C" // "A" 또는 "B" 상태일 때 10이나 20이 아닌 아무 신호나 입력되면 "C"로 전이
any("A", "B") x any(10, 20) %= "C" // "A"나 "B" 상태가 아닐 때 10이나 20이 아닌 아무 신호나 입력되면 "C"로 전이
```
#### 전이규칙 정의 5 - 체이닝 룰
```kotlin
"A" x "S" %= "B"
"B" x "S" %= "C"
"C" x "S" %= "D"
// 위와 같은 전이규칙을 아래같은 체이닝 룰로 정의할 수 있음
chain from "A" to "B" to "C" to "D" via "S"
```
#### 전이규칙 정의 6 - 술어형 신호
```kotlin
"A" x { s: Char -> '0' < s && s < '9' } %= "B" // "A" 상태일 때 Predicate를 만족하는 신호가 입력되면 "B"로 전이
"A" x '0'..'9' %= "B" // Range도 술어형 신호로서 사용 가능
```
#### 머신 콜백
```kotlin
val machine = KotlmataMachine("sample") {
    on error {
        // 머신의 동작 수행 중 에러(예외) 발생 시 호출
        println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
    }
    on transition { from, signal, to ->
        // 상태 전이 발생 시 호출
        // from x signal %= to 규칙으로 전이가 발생했음을 의미
        println(transitionCount) // transitionCount 프로퍼티를 통해 머신에서 몇 번째로 발생한 전이인지 알 수 있음
    } catch { from, signal, to ->
        // 위 블럭에서 예외 발생 시 여기로 빠짐
        println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
    } finally { from, signal, to ->
        // try-catch-finally 구문의 finally와 동일
        // 예외가 발생하든 안하든 무조건 실행됨
    }
}
```

### 상태 정의

#### 상태동작 정의 1 - 기본
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {
        input action { s ->
            // 머신의 현재 상태가 "A"일 때 신호가 입력되면 실행됨
            // 만약 "S"라는 신호가 입력 되었다면 파라미터 s는 "S"
            println(transitionCount) // 동작 진입 시점의 transitionCount를 얻을 수 있음
        }
        exit action { s ->
            // "A" 상태에서 다른 상태로 전이(퇴장) 시 실행됨
            println(nextState) // nextState 프로퍼티를 통해 다음 상태가 무엇인지 알 수 있음
            println(transitionCount) // 퇴장동작 또한 transitionCount 참조 가능
        }
        exit action { s ->
            // 동일한 동작을 중복해서 정의할 경우 기존 동작을 덮어씀
            println(s)
        }
    }
    "B" {
        entry action { s ->
            // 다른 상태에서 "B" 상태로 전이(진입) 시 실행됨
            // 파라미터 s는 상태 전이를 유발한 신호
            println(prevState) // prevState 프로퍼티를 통해 이전 상태가 무엇인지 알 수 있음
            println(transitionCount) // 진입동작 또한 transitionCount 참조 가능
        }
    }
    
    "A" x "S" %= "B"
    // 만약 머신의 현재 상태가 "A"이고 "S"가 입력 된다면
    // 위의 전이규칙으로 인해 상태 "A"에서 "B"로의 전이가 발생하고
    // 아래 순서대로 상태동작이 실행됨
    // 1. "A"의 input action
    // 2. "A"의 exit action
    // 3. "B"의 entry action
    // 이 때 각각의 상태동작에 전달되는 파라미터(신호)는 "S"임
    // 위 세 가지 상태동작을 위에서부터 각각 '입력동작, 퇴장동작, 진입동작'이라 부름
    
    start at "A"
}
```
#### 상태동작 정의 2 - 신호 지정
```kotlin
entry via "S" action { s ->
    // "S"라는 신호를 통해 진입한 경우 실행됨 (진입동작)
    // s == "S"
}
input signal String::class action { s ->
    // String 타입의 신호가 입력된 경우 실행됨 (입력동작)
    // s is String
}
exit via 10 action { s ->
    // 퇴장동작 또한 신호 지정 가능
    // s == 10
}
```
#### 상태동작 정의 3 - 다중 신호 지정
```kotlin
entry via ("S" OR 10) action { s ->
    // "S" 또는 10 신호를 통해 진입한 경우
    // s is SIGNAL(=Any)
}
input signal ("S1" OR "S2") action { s ->
    // s is String
    // s의 타입은 다중 신호들의 최소 상한으로 추론됨
}
```
#### 상태동작 정의 4 - 술어형 신호 지정
```kotlin
entry via { s: Int -> s < 10 } action { s ->
    // 10보다 작은 Int 타입 신호를 통해 진입한 경우
    // s is Int
}
exit via 0..9 action { s ->
    // Range 형태도 가능
}
```
#### 상태동작의 에러 처리
```kotlin
val machine = KotlmataMachine("sample") {
    on error /* 머신 에러처리 */ {
        // 상태에서 에러 처리가 안될 경우 여기로 빠짐
    }
    
    "A" {
        entry action { s ->
            throw Exception() // 예외 발생 시 아래 catch 블럭 실행
        } catch /* 상태동작 에러처리 */ { s ->
            println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
        } finally { s ->
            // 예외가 발생하든 안하든 무조건 실행됨
        }
        input action { s ->
            throw Exception() // '상태동작 에러처리'가 없으므로 아래 '상태 에러처리' 블럭으로 예외가 전파됨
        }
        on error /* 상태 에러처리 */ { s ->
            // 상태동작에서 예외 처리가 되지않은 경우 여기로 빠짐
            println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
        }
    }
    "B" {
        entry action { s ->
            throw Exception() // '상태동작 에러처리', '상태 에러처리' 둘 다 없으므로 '머신 에러처리' 블럭으로 예외 전파
        }
    }
    
    start at "A"
}
```
#### 상태의 정리
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {
        lateinit var resource: SomeResource // 진입동작에서 할당할 것이 자명하므로 lateinit 사용
        entry action { s ->
            resource = SomeResource()
        }
        on clear {
            // 상태 전이 시 퇴장동작 이후 최종적으로 실행됨
            // 머신 해제 시에도 호출됨
            resource.clear() // 할당한 자원을 해제해줌
        }
    }
    
    start at "A"
}
```
#### 미리 정의된 상태 템플릿 활용
```kotlin
val machine = KotlmataMachine("sample") {
    // 상태들의 공통 정의를 미리 템플릿으로 만들어 놓음
    val template: StateTemplate<String/* 상태 태그의 타입 */> = {
        on error { s ->
            println(throwable.message)
        }
        entry action { s ->
            println(s)
        }
    }
    
    "A" extends template with {
        on error { s -> // 템플릿의 on error가 덮어써짐
            println(s)
        }
        // 추가 정의
    }
    "B" extends template // 추가 정의는 생략할 수 있음
    
    start at "A"
}
```

### 머신 제어

#### 신호 입력
```kotlin
val machine = KotlmataMachine("sample") { 
    "A" {}
    "B" {
        entry action { s ->
            // 아래의 입력에 의해 s == 10
        }
    }

    "A" x 0 %= "B"
    
    start at "A"
}

machine.input(10) // 머신에 신호 10을 입력. 머신의 상태는 A에서 B로 전이됨
```
#### 타입 지정 신호 입력
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {
        input signal "signal1" action { s ->
            println("$s is signal1")
        }
        input signal String::class action { s ->
            println("$s is String")
        }
        input signal CharSequence::class action { s ->
            println("$s is CharSequence")
        }
    }
    
    start at "A"
}

machine.input("signal1") // "signal1 is signal1" 출력
machine.input("signal2") // "signal2 is String" 출력
machine.input("signal2", CharSequence::class) // "signal2 is CharSequence" 출력
// 타입 지정은 신호의 상위 타입만 지정 가능
```
#### 페이로드 전달
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {
        input action { s ->
            println(payload) // payload 프로퍼티를 통해 신호와 함께 전달된 페이로드를 얻을 수 있음
        }
        exit action { s ->
            println(payload) // 상태 전이가 발생했다면 exit에도 전달
        }
    }
    "B" {
        entry action { s ->
            println(payload) // 상태 전이 시 최종적으로 entry까지 전달됨
        }
    }

    "A" x 0 %= "B"
    
    start at "A"
}

machine.input(0, "payload")
machine.input("S", String::class, "payload") // 신호, 타입, 페이로드 순
```
#### 머신 해제
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {
        on clear {
            println("상태 A 정리")
        }
    }
    
    start at "A"
}

machine.release() // 머신을 해제함. "상태 A 정리" 출력
// 머신 해제는 머신이 val로 할당되어 null을 대입할 수 없는 경우에
// 머신이 참조하는 자원을 해제하고 싶을 경우 유용함
```

### 머신 및 상태 업데이트

#### 수정 가능한 머신 - KotlmataMutableMachine
```kotlin
// KotlmataMachine은 생성되면 수정이 불가능
// KotlmataMutableMachine은 생성 후 수정(업데이트) 가능함
val machine = KotlmataMutableMachine("sample") { 
    "A" {}
    "B" {}

    "A" x 0 %= "B"
    
    start at "A"
}
machine update {
    // 생성된 머신의 update 함수를 호출하여 업데이트 블럭을 열 수 있음
}
machine {
    // 'update'를 생략하고 바로 블럭을 열 수도 있음
}
```
#### 머신 업데이트 1 - 현재 상태 획득
```kotlin
machine {
    println(currentState) // currentState 프로퍼티를 통해 현재 상태 확인 가능
}
```
#### 머신 업데이트 2 - 상태 및 전이규칙 존재 여부 확인
```kotlin
machine {
    has state "A" // 머신에 상태 "A"가 존재하면 true
    has rule ("A" x "S") // 머신에 좌변이 "A" x "S"인 전이규칙이 존재하면 true
    has rule ("A" x String::class) // 가능
    has rule (any x String::class) // 가능
    // 상태와 신호가 일대일인 전이규칙의 좌변은 모두 가능
    // has rule ("A" x ("S" OR "10)) 는 불가능
    // has rule (any("A", "B") x "S") 는 불가능
}
```
#### 머신 업데이트 3 - 상태 및 전이규칙 삭제
```kotlin
machine {
    delete state "A" // 머신에서 "A" 상태 삭제
    delete state all // 머신의 모든 상태 삭제
    delete rule ("A" x "S") // 좌변이 "A" x "S"인 전이규칙 삭제
    delete rule all // 머신의 모든 전이규칙 삭제
}
```
#### 상태 업데이트 1 - update 함수
```kotlin
val machine = KotlmataMutableMachine("sample") { 
    "A" {
        entry action { s ->
            println(s)
        }
    }
    "A" {
        input action { s ->
            println(s)
        }
    } // 이렇게 하면 처음에 정의한 "A"는 완전히 사라지고 뒤에 정의한 "A"로 대체됨
    // 기존의 상태 정의를 살리면서 새로운 동작을 추가하려면 아래처럼 update 함수를 사용
    "A" update {
        // 추가 정의
    }
    start at "A"
}

machine {
    "A" update {
        // 머신의 업데이트 블럭에서도 상태 업데이트 가능
    }
}
```
#### 상태 업데이트 2 - 상태동작 삭제
```kotlin
"A" update {
    delete action entry // 기본 진입동작(entry action{}) 삭제
    delete action entry via "S" // "S"로 신호 지정된 진입동작 삭제
    delete action entry via all // 신호 지정된 모든 진입동작 삭제 (기본 진입동작은 삭제되지 않음)
    delete action input signal "S" // 입력동작 삭제도 동일한 문법으로 가능
    delete action all // 모든 상태동작 삭제
}
```

### 상태함수와 동기입력

#### 상태함수
```kotlin
"A" {
    entry function /* 상태함수는 action이 아닌 function을 사용 */ {
        // 진입함수
        0 // 신호를 리턴할 수 있음
    }
    input signal "S" function {
        0 // 입력함수도 정의 가능
    }
    // exit function(퇴장함수)는 불가능
}
```
#### 동기입력
```kotlin
"A" {
    entry via 0 function {
        "S1" // 상태함수의 리턴값은 머신의 바로 다음 입력으로 사용되고 이를 동기입력이라 표현함
    }
    input signal "S1" function {
        "S2" `as` CharSequence::class // `as` 함수를 써서 타입 지정 동기입력 가능
    }
    input signal "S2" function {
        "S3" with "payload" // with 함수로 페이로드도 전달 가능
    }
    input signal "S3" function {
        "S4" `as` CharSequence::class with "payload" // 타입 지정, 페이로드 전달 동시에 가능
    }
}
```
#### 동기입력과 상태 전이
```kotlin
val machine = KotlmataMachine("sample") {
    "A" {
        input signal 1 function {
            // 현재 1이 입력된 상태이고
            // "A" x 1 %= "C" 규칙에 의해 "C"로 전이해야 하지만
            10 // 동기입력이 전이규칙 실행을 블럭하고 10이 바로 머신에 입력됨
        }
    }
    "B" {}
    "C" {}

    "A" x 1 %= "B"
    "B" x 1 %= "C"
    "A" x 10 %= "C"
    "C" x 1 %= "B"
    
    start at "A"
}

machine.input(1)
machine.input(1)
// 만약 동기입력이 없다면 머신의 상태는 "A" -> "B" -> "C"로 전이
// 하지만 "A"의 입력함수 동기입력 때문에 실제 상태 전이는 "A" -> "C" -> "B"로 발생함
```
#### 상태함수의 예외 처리 - intercept
```kotlin
"A" {
    entry function {
        throw Exception()
        0 // 위의 예외로 인해 0이 리턴되지 않음
    } intercept {
        // intercept는 상태함수에만 사용 가능. 상태동작(action)에는 사용 불가
        1 // 상태함수처럼 신호를 리턴할 수 있음. 동기입력으로 사용
    }
}
```

### 상태동작 및 전이규칙 실행의 우선순위

#### 상태동작 선택의 우선순위
```kotlin
"A" {
    input signal "A" action { s ->
        // 객체 신호
        // 신호 입력 시 가장 먼저 테스트됨
        // 신호가 "A"이면 실행되고 s is String
    }
    input signal "A".."Z" action { s ->
        // 술어형 신호
        // 객체 신호 실패 시 술어형 신호가 두 번째로 테스트됨
        // 신호가 "B"이면 실행되고 s is String
    }
    input signal String::class action { s ->
        // 타입 신호
        // 객체, 술어형 신호가 실패하면 세 번째로 테스트됨
        // 신호가 "a"이면 실행되고 s is String
    }
    input action { s ->
        // 기본형
        // 모든 신호 지정 테스트가 실패하면 실행됨
        // 신호가 10이면 실행되고 s is SIGNAL(=Any)
    }
}
```
#### 전이규칙 선택의 우선순위
```kotlin
"A" x 10 %= "B" // 가장 우선순위가 높음
"A" x 0..10 %= "B" // 두 번째 우선순위
"A" x Int::class %= "B" // 세 번째
"A" x any %= "B" // 네 번째
"A" x any(0, 1) %= "B" // 네 번째와 동일. 동일한 좌변이 중복 정의된 것으로 처리됨(이전 규칙을 덮어씀)
any x 10 %= "B" // 다섯 번째
any x 0..10 %= "B" // 여섯 번째
any x Int::class %= "B" // 일곱 번째
any x any %= "B" // 여덟 번째. 가장 우선순위가 낮음
```

### 머신 기타

#### 상태 전이와 TransitionCount 증가
```kotlin
val machine = KotlmataMachine("sample") {
    on transition { from, signal, to ->
        println("$transitionCount: $from x $signal -> $to")
    }
    "A" { state ->
        entry action { signal ->
            println("$transitionCount: $state entry via $signal")
        }
        input action { signal ->
            println("$transitionCount: $state input signal $signal")
        }
        exit action { signal ->
            println("$transitionCount: $state exit via $signal")
        }
    }
    "B" { state ->
       entry action { signal ->
            println("$transitionCount: $state entry via $signal")
        }
        input action { signal ->
            println("$transitionCount: $state input signal $signal")
        }
        exit action { signal ->
            println("$transitionCount: $state exit via $signal")
        }
    }

    "A" x any %= "B"
    "B" x any %= "A"
    
    start at "A"
}
machine.input(0)
machine.input(1)
```
로그는 아래와 같음
```
0: A input signal 0
0: A exit via 0
1: A x 0 -> B
1: B entry via 0
1: B input signal 1
1: B exit via 1
2: B x 1 -> A
2: A entry via 1
```

## KotlmataDaemon

### 데몬 개요

- 데몬은 머신을 베이스로 함
- 내부적으로 별도의 쓰레드를 생성함
- 요청에 대한 큐를 소유하고 생산자-소비자 방식으로 동작
- 머신과 달리 쓰레드 안전함
- 생명주기를 지님

### 데몬 생성

#### 기본형
```kotlin
val daemon = KotlmataDaemon(
    "sample", /* 로그에 출력되는 데몬의 이름 */
    2, /* 로그레벨 */
    "threadName", /* 데몬이 생성할 쓰레드 이름 */
    false /* 쓰레드의 데몬쓰레드(isDaemon) 여부 */
) {
    // 데몬 정의
}

val mutableDaemon = KotlmataMutableDaemon("sample") { // Mutable 타입도 생성가능
    // 데몬 정의
}
```
#### Lazy형
```kotlin
val daemon by KotlmataDaemon.lazy("sample") {
    // 데몬 정의
}
```
#### 미리 정의된 데몬이 있는 경우
```kotlin
val define: DaemonDefine = {
    // 데몬 정의
}
val daemon = KotlmataDaemon("sample") by define
```
#### 미리 정의된 템플릿이 있는 경우
```kotlin
val template: DaemonTemplate = {
    // 데몬 정의
    // 시작 상태는 정의할 수 없음
}
val daemon = KotlmataDaemon("sample") extends template by {
    // 추가 데몬 정의
}
```

### 데몬 정의

#### 데몬 정의
```kotlin
val daemon = KotlmataDaemon("sample") {
    // 머신에서 할 수 있는 모든 정의에 추가로 생명주기 관련 정의를 할 수 있음
    on create {
        // 데몬 생성 시 실행
    } catch {
        // 위 블럭에서 예외 발생 시 여기로 빠짐
        println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
    } finally {
        // try-catch-finally 구문의 finally와 동일
        // 예외가 발생하든 안하든 무조건 실행됨
        // catch-finally 인터페이스는 아래의 나머지 생명주기 정의도 모두 동일
    }
    on start {
        // 데몬 시작 시 실행
        println(payload) // 제어함수를 통해 전달받은 페이로드
    }
    on pause {
        // 데몬 일시정지 시 실행
        println(payload) // 제어함수를 통해 전달받은 페이로드
    }
    on stop {
        // 데몬 정지 시 실행
        println(payload) // 제어함수를 통해 전달받은 페이로드
    }
    on resume {
        // 데몬 재개(일시정지 혹은 정지 상태에서 다시 시작) 시 실행
        println(payload) // 제어함수를 통해 전달받은 페이로드
    }
    on finish {
        // 데몬 종료 시 실행
        // on start와 짝을 이룸
        println(payload) // 제어함수를 통해 전달받은 페이로드
    }
    on destroy {
        // 데몬 소멸 시 실행
        // on create와 짝을 이룸
    }
    
    on error {
        // 머신의 on error와 동일
        println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
    }
    on fatal {
        // on error 블럭 진입은 예외처리만 적절히 해주면 데몬 구동에는 문제가 없는 상황임
        // 반면 on fatal 블럭 진입은 더이상 데몬 구동이 불가능한 상황을 의미함
        // 이 블럭을 벗어나면 데몬은 자동으로 종료되고 소멸할 것임
        println(throwable.message) // throwable 프로퍼티를 통해 예외를 얻을 수 있음
    }
    
    "A" {
        // 상태 정의
    }
    
    start at "A"
}
```

### 데몬 제어 및 생명주기

#### 신호 입력
```kotlin
// 기본적인 시그니처는 머신과 동일
daemon.input("S")
daemon.input("S", String::class)
daemon.input("S", String::class, "payload")

// 데몬 입력은 마지막 파라미터로 우선순위를 지정할 수 있음
daemon.input("S", priority = 0) // priority값이 작을수록 우선순위가 높음
daemon.input("S", String::class, priority = 1)
daemon.input("S", String::class, "payload", 2)
```
#### 데몬 업데이트
```
val daemon = KotlmataMutableDaemon("sample") {
    // 데몬 정의
}

daemon {
    // 머신 업데이트 블럭과 동일
}
```
#### 데몬 제어
```kotlin
daemon.run("payload") // 데몬을 시작함. 모든 제어함수에는 페이로드를 전달할 수 있고 이는 생명주기 콜백에 전달됨
daemon.pause() // 데몬을 일시정지함
daemon.stop() // 데몬을 정지함
daemon.terminate() // 데몬을 종료함
```
#### 생명주기 그래프
![enter image description here](https://user-images.githubusercontent.com/3992883/105116267-bdaf0b00-5b0d-11eb-8b8e-4766714ba1d2.png)
> "create"와 "destroy"는 데몬 내부적으로 사용하는 신호

### 요청 큐와 우선순위

#### 요청의 종류와 우선순위
`daemon.xxx()`로 호출되는 모든 함수는 요청이라는 형태로 데몬의 요청 큐에 추가됨.  
요청의 종류는 우선순위가 높은 순으로 아래와 같음.
1. 머신제어 요청 - `daemon.run()`등의 생명주기 제어 요청
2. 업데이트 요청 - `daemon update {}`, `daemon {}`
3. 동기입력 요청 - 상태함수에서 동기입력을 리턴하는 경우 데몬 내부적으로 요청 생성
4. 신호입력 요청 - `daemon.input()`. 입력 신호들 간에 우선순위를 지정할 수 있음.
#### 요청 큐 예시 1 - 일반적인 입력
```kotlin
val daemon = KotlmataMutableDaemon("sample") {
    "A" {
        input signal 1 function {
            // 오래 걸리는 작업
            5 // 동기입력 리턴
        }
    }
    start at "A"
}

daemon.input(1, priority = 10)
daemon.input(2, priority = 10)
daemon.input(3, priority = 10)
```
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  |  |  |  | *3 | *2 | *1 |
#### 요청 큐 예시 2 - 우선순위가 높은 신호 입력
```kotlin
daemon.input(4, priority = 1)
```
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  |  |  | 3 | 2 | *4 | 1 |
#### 요청 큐 예시 3 - pause 요청
```kotlin
daemon.pause()
```
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  |  | 3 | 2 | 4 | *pause | 1 |
#### 요청 큐 예시 4 - 동기입력
신호 1에 대한 입력함수가 끝나고 동기입력 5가 입력됨
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  |  | 3 | 2 | 4 | *5 | pause |
#### 요청 큐 예시 5 - pause 상태에서 요청
pause 상태일 때 요청이 들어오면 그대로 큐에 쌓임
```kotlin
daemon.input(6, priority = 5)
```
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  | 3 | 2 | *6 | 4 | 5 | pause |
#### 요청 큐 예시 6 - stop 요청
데몬이 stop 되면 큐에서 신호입력이 모두 제거되고 추가적인 신호입력도 받지 않음  
동기입력, 업데이트, 머신제어 요청은 그대로 유지됨
```kotlin
daemon.stop()
daemon.input(7)
```
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  |  |  |  |  | 5 | *stop |
#### 요청 큐 예시 6 - 재개
```kotlin
daemon.run()
daemon.input(8)
daemon.input(9)
```
| 5 | 4 | 3 | 2 | 1 | 0 | 작업중 |
|--|--|--|--|--|--|--|
|  |  |  |  | *9 | *8 | *5 |

# License

```$xslt
Copyright 2018 Jongsun Yoo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```