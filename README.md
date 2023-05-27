# BlockCodeHITs
# developers: 
<ul>
  <li>Name: Ivanov Vitaliy - ProNagibator2000
  <li>Subject area: Interpreter
</ul>
<ul>
  <li>Name: Seletsky Vlad - AtLeastIHaveASandwich
  <li>Subject area: UI/UX
</ul>

![123](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/fdd71c62-060d-45eb-b9a9-bcf105877836) <br>

There are 4 types of variables:
<ul>
  <li>int
  <li>double
  <li>boolean
  <li>char
</ul>
There is also support for arrays consisting of elements of these types

To declare a variable, you need to specify the type and name:  
<ul>
  <li>int a
  <li>char ch
</ul>

You can also specify a non-zero starting value of the variable, for this the '=' operator is used:
<ul>
  <li>int a = 323
  <li>double b = 32.3 + (52.2 - 1.0)
  <li>boolean flag = false
  <li>char ch = 'c'
</ul>

To declare arrays, you must specify the type name and specify the number of elements inside the '[]' operator
<ul>
  <li>int a[32]
  <li>double b[10]
  <li>boolean flag[2]
  <li>char ch[45]
</ul>

In addition, an array of type char is a string, and you can assign a string to it without specifying the number of elements:
<ul>
  <li>char ch[] = "String"
  <li>char ch[45] = "Temple"
</ul>

To change the value of a variable, array element, or string value in the set block, specify its name and use the '=' operator to assign a new value to it:
<ul>
  <li>a = 32 + 3 % (3 / 2) + b - a
  <li>arr[i+1] = arr[i] + 4 - 1
  <li>str = "new string"
</ul>

To convert the variable type, you need to specify the operators toInt, .toDouble, .toBoolean or .toChar before the operand:
<ul>
  <li>a = 32.toDouble
  <li>arr[i+1] = arr[i].toBoolean
  <li>str[j] = 48.toChar
  <li>str[j] = 48.6.toInt
</ul>

![124](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/2626a378-bf24-43b0-a609-50d5ef0467b8) <br>
For the if block to work, you must specify a boolean expression, for this you can use the operators ||, &&, ==, !=, <=, >=, <, >, !
<ul>
  <li> 1 == 213 - 212
  <li> arr[i] > arr[j] || true
  <li> !false
</ul>

![image](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/a4dd0e0c-af59-45b4-bb32-a95df8cd6294) <br>
The while block, as well as if, accepts a boolean expression.
For the for block to work, you must specify three commands separated by two operators ':'
<ul>
  <li> init; condition; assignment
</ul>
The init command is executed like the var block, the condition must be specified in the same way as in the while block, the command works like the set block and is executed after the first iteration:
<ul>
  <li> int i = 1; i <= 10; i = i + 1
</ul>
    
![image](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/8a19f8f0-79e8-44f3-bdee-db027a23226d) <br>
To declare a function, you need to specify the type of the return value, and arguments with types and names:
<ul>
  <li> int function(int x, double y, boolean flag)
</ul>
Also, each function must return a value using the return block:
<ul>
  <li> 0
  <li> 5
</ul>
You can call the function in the set, if, while, for and return blocks, for this you need to specify the name of the function operator '()', inside which you need to specify the values to be passed:
<ul>
  <li> block set: a = f(f(1, 1.0, false), 2.0, true) + 2
  <li> block return or if, for, while: f(1, 1.0, false) <= f(3, 1.0, true) || f(1, 1.0, false) == 0
</ul>

![image](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/885856d3-498a-42d3-b712-c67d05e47618) <br>
To work with the debug, you need to choose a place where you need to stop, for this you need to click on the break point button, then it will turn green.
   
    
![image](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/a6475d9c-355e-4f9a-ba7b-1858dea4bdfd)
The block where the program stopped is highlighted in orange. To move to the next block, click on the button with one arrow. To move to the next breakpoint, click on the double arrow button. To view the variables that are visible to the block where the program stopped, click on the button with the eye:
![image](https://github.com/ProNagibator2000/BlockCodeHITs/assets/120786249/048d704f-a3b1-4943-aa00-ae9fd1c4d6a2) <br>
    



