import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

export default function InventarioItem({ item, onEdit, onDelete }) {
    const valorFormatado = Number(item.valorTotal || 0).toFixed(2);

    return (
        <View style={styles.card}>
            <View style={styles.info}>
                <Text style={styles.nome}>{item.nome}</Text>
                {item.descricao ? (
                    <Text style={styles.descricao}>{item.descricao}</Text>
                ) : null}
                <Text style={styles.valor}>Valor total: R$ {valorFormatado}</Text>
            </View>

            <View style={styles.acoes}>
                <TouchableOpacity style={styles.botaoEditar} onPress={() => onEdit(item)}>
                    <Text style={styles.textoBotao}>Editar</Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.botaoExcluir} onPress={() => onDelete(item)}>
                    <Text style={styles.textoBotao}>Excluir</Text>
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    card: {
        backgroundColor: '#1E1E24',
        borderRadius: 8,
        padding: 14,
        marginBottom: 10,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    info: {
        flex: 1,
        paddingRight: 10,
    },
    nome: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: 'bold',
    },
    descricao: {
        color: '#A1A1AA',
        fontSize: 13,
        marginTop: 4,
    },
    valor: {
        color: '#34D399',
        fontSize: 13,
        fontWeight: '600',
        marginTop: 6,
    },
    acoes: {
        flexDirection: 'row',
        gap: 8,
    },
    botaoEditar: {
        backgroundColor: '#2563EB',
        paddingVertical: 6,
        paddingHorizontal: 10,
        borderRadius: 6,
    },
    botaoExcluir: {
        backgroundColor: '#DC2626',
        paddingVertical: 6,
        paddingHorizontal: 10,
        borderRadius: 6,
    },
    textoBotao: {
        color: '#FFFFFF',
        fontSize: 12,
        fontWeight: '600',
    },
});