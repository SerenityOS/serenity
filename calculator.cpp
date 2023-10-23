#include <iostream>
#include <complex>

using namespace std;

int main() {
    complex<double> num1, num2;
    char operation;

    cout << "Enter the first number (a + bi): ";
    cin >> num1;

    cout << "Enter the operation (+, -, *, /, c for complex conjugate): ";
    cin >> operation;

    if (operation != 'c') {
        cout << "Enter the second number (a + bi): ";
        cin >> num2;
    }

    complex<double> result;

    switch (operation) {
        case '+':
            result = num1 + num2;
            break;
        case '-':
            result = num1 - num2;
            break;
        case '*':
            result = num1 * num2;
            break;
        case '/':
            if (num2 != 0.0) {
                result = num1 / num2;
            } else {
                cout << "Error! Division by zero." << endl;
                return 1;
            }
            break;
        case 'c':
            result = conj(num1);
            break;
        default:
            cout << "Error! Invalid operation." << endl;
            return 1;
    }

    cout << "Result: " << result << endl;

    return 0;
}
